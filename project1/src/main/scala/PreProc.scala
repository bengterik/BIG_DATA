package flight_predictor

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql._

object PreProc {
    def loadDf(spark: SparkSession, path: String): Dataset[Row] = {
      val df = loadFromFile(spark, path)
                
      removeUnwantedColumns(formatColumns(df))           
    }

    def formatColumns(ds: Dataset[Row]): Dataset[Row] = {
        formatIntoHours(
            ds.withColumn("Month", ds.col("Month").cast("Integer"))
                .withColumn("DayOfWeek", ds.col("DayOfWeek").cast("Integer"))
                .withColumn("CRSElapsedTime", ds.col("CRSElapsedTime").cast("Integer"))
                .withColumn("ArrDelay", ds.col("ArrDelay").cast("Integer"))
                .withColumn("Distance", ds.col("Distance").cast("Integer"))
                .withColumn("TaxiOut", ds.col("TaxiOut").cast("Integer")), 
            "CRSDepTime")
    }

    def loadFromFile(spark: SparkSession, path: String): Dataset[Row] = {
        val fromFile = spark.read.csv(path)
        val headings = fromFile.take(1)(0).toSeq.map(_.toString)
        val rowToSkip = fromFile.first
        fromFile.filter(row => row != rowToSkip).toDF(headings:_*)
    }

    def removeUnwantedColumns(ds: Dataset[Row]): Dataset[Row] = {
        val forbiddenColumns = List("ArrTime", "DepTime", "ActualElapsedTime", "AirTime", "TaxiIn", "Diverted", "CarrierDelay", "WeatherDelay", "NASDelay", "SecurityDelay", "LateAircraftDelay")
        val excludedColumns = List("Year", "DayOfMonth", "FlightNum", "CRSArrTime", "Cancelled", "TailNum", "DepDelay", "Cancelled", "CancellationCode", "UniqueCarrier", "Origin", "Dest")
        ds.filter(ds("Cancelled") < 1)
          .drop((forbiddenColumns ++ excludedColumns): _*)
          .na.drop
    }

    /*
    Example showing how to turn 16.33 into 16.55 in order to get correct rounding to hours:
        Round ( [16.33 - floor(16.33)] * 1.6666667 + floor(16.33) ) =
        Round ( [16.33 - 16.00] * 1.6666667 + 16.00 ) = 16.55
    Also: modulo is added in the end to avoid having an hour bigger than 23. 
    */
    def formatIntoHours(ds: Dataset[Row], column: String): Dataset[Row] = {
        ds.withColumn(column, 
            round((ds(column).cast("Integer") / 100 - floor(ds(column).cast("Integer") / 100)) 
            * 1.666666667 
            + floor(ds(column).cast("Integer") / 100)) % 24
            )
    }
}
