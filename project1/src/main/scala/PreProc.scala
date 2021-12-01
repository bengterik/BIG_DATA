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
    def loadDf(spark: SparkSession): Dataset[Row] = {
      val fromFile = spark.read.csv("src/main/resources/2008.csv")
      val rowToSkip = fromFile.first
      val headingsAll = fromFile.take(1)(0).toSeq.map(_.toString)

      val dfWithCancelled = fromFile.filter(row => row != rowToSkip).toDF(headingsAll:_*)
      val dfWithoutCancelled = dfWithCancelled.filter(dfWithCancelled("Cancelled") < 1)
                       
      val forbiddenColumns = List("ArrTime", "DepTime", "ActualElapsedTime", "AirTime", "TaxiIn", "Diverted", "CarrierDelay", "WeatherDelay", "NASDelay", "SecurityDelay", "LateAircraftDelay")
      val excludedColumns = List("Year", "DayOfMonth", "FlightNum", "TailNum", "DepDelay", "Cancelled", "CancellationCode", "UniqueCarrier", "Origin", "Dest")
      val tableCountMissingValues = dfWithoutCancelled.select(dfWithoutCancelled.columns.map(c => sum(col(c).isNull.cast("int")).alias(c)): _*)
      val features = dfWithoutCancelled.columns.filterNot(_== "ArrDelay")
      dfWithoutCancelled.drop((forbiddenColumns ++ excludedColumns): _*)
                .withColumn("CRSDepTime", doubleValues(spark, dfWithoutCancelled, "CRSDepTime"))
                .withColumn("CRSArrTime", doubleValues(spark, dfWithoutCancelled, "CRSArrTime"))
    }

    def doubleValues(sparkSesh: SparkSession, dataset: Dataset[Row], column: String): Column = {
        import sparkSesh.implicits._
        dataset.select(column).map(row => normalizeTime(row.getString(0))).toDF("value")("value")
    }
    
    def normalizeTime(sIn: String): Double = {
        // prepending to have consistent size)
        val s = "0" * (4 - sIn.length) + sIn
        
        val s1 = s.substring(0,2)
        val s2 = s.substring(2,4)

        val i1 = Integer.parseInt(s1)
        val i2 = Integer.parseInt(s2)
        
        (i1*60 + i2)/(24*60).toDouble
    }
}