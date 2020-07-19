package org.cdl.iot

import java.io.File

import org.apache.spark.SparkConf
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions.{to_timestamp, _}
import org.apache.spark.sql.types.DoubleType
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.jpmml.sparkml.PMMLBuilder

case class Model(
                  key: String,
                  model: Array[Byte]
                )

object ModelTraining {


  def main(args: Array[String]): Unit = {

    val conf = new SparkConf()
      .setAppName("baselineModel")
      .set("spark.cassandra.connection.host", "127.0.0.1")
      .set("spark.cassandra.auth.username", "cassandra")
      .set("spark.cassandra.auth.password", "cassandra")
      .setMaster("local[2]")
    val spark = SparkSession.builder.config(conf).getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    val file_path = s"${System.getProperty("user.dir")}/data/operable.csv"
    val time_path = s"${System.getProperty("user.dir")}/data/time.csv"

    val ts = to_timestamp(col("timestamp"), "yy-MM-dd HH:mm")

    val data = spark
      .read
      .format("CSV")
      .option("header", "true")
      .load(file_path)
      .withColumn("timestamp", ts)
      .sort("timestamp")

    val time = spark
      .read
      .format("CSV")
      .option("header", "true")
      .load(time_path)
      .withColumn("from", to_timestamp(col("from"), "yy-MM-dd HH:mm")).sort("from")
      .withColumn("to", to_timestamp(col("to"), "yy-MM-dd HH:mm"))


    data.show(5)
    time.show(5)

    // compare time intervals
    val transformed = data
      .withColumn("end_hour", col("timestamp") + expr("INTERVAL 30 minutes"))
      .join(time,
        col("end_hour") > time("from")
          && col("end_hour") < time("to"),
        "left"
      )
      .withColumn("operable", when(isnull(col("from")), 1).otherwise(0))



    val transformedDataSet = transformed
      .sort("timestamp")
      .withColumn("temperature", col("temperature").cast(DoubleType))
      .withColumn("pressure", col("pressure").cast(DoubleType))
      .withColumn("wind", col("wind").cast(DoubleType))
      .drop("from")
      .drop("to")

    transformed.show(5)
    transformedDataSet.show(5)

    // select features
    val assembler = new VectorAssembler()
      .setInputCols(Array("temperature", "pressure", "wind"))
      .setOutputCol("features")
      .setHandleInvalid("skip")

    val labelIndexer = new StringIndexer().setInputCol("operable").setOutputCol("label")


    // split train and test data
    val cnt = transformedDataSet.count
    val testSize = (0.2 * cnt).toInt
    val trainSize = (cnt - testSize).toInt

    val trainDf = transformedDataSet.sort(monotonically_increasing_id).limit(trainSize)
    val testDf = transformedDataSet.sort(monotonically_increasing_id.desc).limit(testSize)

    val classifier = new RandomForestClassifier()
      .setImpurity("gini")
      .setMaxDepth(3)
      .setNumTrees(20)
      .setFeatureSubsetStrategy("auto")
      .setSeed(813)

    val pipeline = new Pipeline().setStages(Array[PipelineStage](labelIndexer, assembler, classifier))
    val model = pipeline.fit(trainDf)

    val predictions = model.transform(testDf)
    //    predictions.select("sensor_id", "label", "prediction").show(5)

    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("label")
      .setPredictionCol("prediction")
      .setMetricName("accuracy")
    val accuracy = evaluator.evaluate(predictions)
    val evaluator2 = new MulticlassClassificationEvaluator()
      .setLabelCol("label")
      .setPredictionCol("prediction")
      .setMetricName("f1")

    val f1 = evaluator2.evaluate(predictions)
    println("F1 score = " + f1)
    println("Test Accuracy = " + accuracy)

    val schema = transformedDataSet.schema
    val dto = Model(
      new DateTime().toString(DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss")),
      new PMMLBuilder(schema, model).buildByteArray() //.buildFile(new File(s"${System.getProperty("user.dir")}/model.pmml"))
    )

    import spark.implicits._

    val ds = Seq(dto).toDS

    ds
      .write
      .format("org.apache.spark.sql.cassandra")
      .options(
        Map(
          "keyspace" -> "iot_prototype_training",
          "table" -> "models")
      )
      .mode(SaveMode.Append)
      .save


  }


}
