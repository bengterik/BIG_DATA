package flight_predictor

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.VectorAssembler

object PreProc {
    def loadDf(spark: SparkSession): org.apache.spark.sql.Dataset[org.apache.spark.sql.Row] = {
      val fromFile = spark.read.csv("src/main/resources/2008.csv")
      val rowToSkip = fromFile.first
      val headingsAll = fromFile.take(1)(0).toSeq.map(_.toString)

      val dfWithCancelled = fromFile.filter(row => row != rowToSkip).toDF(headingsAll:_*)
      val dfWithoutCancelled = dfWithCancelled.filter(dfWithCancelled("Cancelled") < 1)
                       
      val forbiddenColumns = List("ArrTime", "DepTime", "ActualElapsedTime", "AirTime", "TaxiIn", "Diverted", "CarrierDelay", "WeatherDelay", "NASDelay", "SecurityDelay", "LateAircraftDelay")
      val excludedColumns = List("Year", "DayOfMonth", "FlightNum", "TailNum", "DepDelay", "Cancelled", "CancellationCode")
      val tableCountMissingValues = dfWithoutCancelled.select(dfWithoutCancelled.columns.map(c => sum(col(c).isNull.cast("int")).alias(c)): _*)
      val features = dfWithoutCancelled.columns.filterNot(_== "ArrDelay")
      dfWithoutCancelled.drop((forbiddenColumns ++ excludedColumns): _*)
 }
}