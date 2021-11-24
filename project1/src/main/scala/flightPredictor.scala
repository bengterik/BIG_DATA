import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object MyApp {
 def main(args : Array[String]): Unit = {
      Logger.getLogger("org").setLevel(Level.WARN)
      
      val df = loadDf()
      
      df.show(3)
 }

 def loadDf(): org.apache.spark.sql.Dataset[org.apache.spark.sql.Row] = {
      val conf = new SparkConf().setAppName("My first Spark application")
      val sc = new SparkContext(conf)
      val spark = SparkSession.builder().appName("Spark SQL project").config("some option", "value").enableHiveSupport().getOrCreate()
      import spark.implicits._
      
      val fromFile = spark.read.csv("src/main/resources/2008.csv")
      val rowToSkip = fromFile.first
      val headings = fromFile.take(1)(0).toSeq.map(_.toString)
      
      val dfWithCancelled = fromFile.filter(row => row != rowToSkip).toDF(headings:_*)
      val df = dfWithCancelled.filter(df("Cancelled") < 1)
                       
      val forbiddenColumns = List("ArrTime", "DepTime", "ActualElapsedTime", "AirTime", "TaxiIn", "Diverted", "CarrierDelay", "WeatherDelay", "NASDelay", "SecurityDelay", "LateAircraftDelay")
      val excludedColumns = List("Year", "DayOfMonth", "FlightNum", "TailNum", "DepDelay", "Cancelled", "CancellationCode")

      df.drop((forbiddenColumns ++ excludedColumns): _*)
 }
}
