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
                
      removeUnwantedColumns(castColumns(df))              
    }

    def castColumns(ds: Dataset[Row]): Dataset[Row] = {
        ds.withColumn("Month", ds.col("Month").cast("Integer"))
        .withColumn("DayOfWeek", ds.col("DayOfWeek").cast("Integer"))
        .withColumn("CRSElapsedTime", ds.col("CRSElapsedTime").cast("Integer"))
        .withColumn("ArrDelay", ds.col("ArrDelay").cast("Integer"))
        .withColumn("Distance", ds.col("Distance").cast("Integer"))
        .withColumn("TaxiOut", ds.col("TaxiOut").cast("Integer"))
    }

    def loadFromFile(spark: SparkSession, path: String): Dataset[Row] = {
        val fromFile = spark.read.csv(path)
        val headings = fromFile.take(1)(0).toSeq.map(_.toString)
        val rowToSkip = fromFile.first
        fromFile.filter(row => row != rowToSkip).toDF(headings:_*)
    }

    def removeUnwantedColumns(ds: Dataset[Row]): Dataset[Row] = {
        val forbiddenColumns = List("ArrTime", "DepTime", "ActualElapsedTime", "AirTime", "TaxiIn", "Diverted", "CarrierDelay", "WeatherDelay", "NASDelay", "SecurityDelay", "LateAircraftDelay")
        val excludedColumns = List("Year", "DayOfMonth", "FlightNum", "TailNum", "DepDelay", "Cancelled", "CancellationCode", "UniqueCarrier", "Origin", "Dest")
        val badlyTypedColumns = List("CRSDepTime", "CRSArrTime")
        ds.drop((forbiddenColumns ++ excludedColumns ++ badlyTypedColumns): _*)
                .filter(ds("Cancelled") < 1)
                .na.drop
    }

    def doubleValues(sparkSesh: SparkSession, dataset: Dataset[Row], column: String): DataFrame = {
        import sparkSesh.implicits._
        dataset.select(column).map(row => normalizeTime(row.getString(0))).toDF()
    }
    
    def normalizeTime(sIn: String): Double = {
        val s = "0" * (4 - sIn.length) + sIn
        
        val s1 = s.substring(0,2)
        val s2 = s.substring(2,4)

        val i1 = Integer.parseInt(s1)
        val i2 = Integer.parseInt(s2)
        
        (i1*60 + i2)/(24*60).toDouble
    }
}
