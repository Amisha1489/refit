name := "models"

version := "1.0"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.4.5",
  "org.apache.spark" %% "spark-sql" % "2.4.5",
  "org.apache.spark" %% "spark-mllib" % "2.4.5"
)

libraryDependencies += "org.jpmml" % "jpmml-sparkml" % "1.5.1"
libraryDependencies += "ml.combust.mleap" %% "mleap-spark-extension" % "0.16.0"