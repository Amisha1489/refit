package edu.cdl.iot.db.reset

import edu.cdl.iot.db.reset.dto.{OperableData, SensorData}
import edu.cdl.iot.db.reset.schema.definitions.Prototype
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Encoders, SaveMode, SparkSession}


object Main {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setAppName("baselineModel")
      .set("spark.cassandra.connection.host", "127.0.0.1")
      .set("spark.cassandra.auth.username", "cassandra")
      .set("spark.cassandra.auth.password", "cassandra")
      .setMaster("local[2]")
    val spark = SparkSession.builder.config(conf).getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

    val schema = Prototype.dummy

    val file_path = s"${System.getProperty("user.dir")}/db/data/${schema.name}.csv"
    val time_path = s"${System.getProperty("user.dir")}/db/data/time.csv"

    val containsHeader = true
    val data = spark
      .read
      .format("CSV")
      .option("header", containsHeader)
      .load(file_path)
      .map(d => {
        val key = schema.getKey(d)
        val timestamp = schema.getTimestamp(d)
        val features = schema.getFeatures(d)
        val labels = schema.getLabels(d)
        SensorData(
          key,
          timestamp,
          features,
          labels
        )
      })(Encoders.product[SensorData])

    val time = spark
      .read
      .format("CSV")
      .option("header", "true")
      .load(time_path)
      .map(d =>
        OperableData(
          d(0).toString,
          d(1).toString,
          d(2).toString,
          d(3).toString
        ))(Encoders.product[OperableData])


    data.show(5)
    time.show(5)

    time
      .write.format("org.apache.spark.sql.cassandra")
      .options(
        Map(
          "keyspace" -> "iot_prototype_training",
          "table" -> "in_operable_entry")
      )
      .mode(SaveMode.Append)
      .save

    data
      .write.format("org.apache.spark.sql.cassandra")
      .options(
        Map(
          "keyspace" -> "iot_prototype_training",
          "table" -> "sensor_data")
      )
      .mode(SaveMode.Append)
      .save

  }
}
