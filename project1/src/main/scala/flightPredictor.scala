package flight_predictor

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.VectorAssembler
import PreProc._

object MyApp {
 def main(args : Array[String]): Unit = {
      val conf = new SparkConf().setAppName("My first Spark application")
      val sc = new SparkContext(conf)
      val spark = SparkSession.builder().appName("Spark SQL project").config("some option", "value").enableHiveSupport().getOrCreate()
      Logger.getLogger("org").setLevel(Level.WARN)
      import spark.implicits._

      val path = "src/main/resources/2008.csv"

      val df = loadDf(spark, path)
      println(ML.mlModel(spark, df, "ArrDelay", 0.7, 0.3))
 }
}
