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
      val badlyTypedColumns = List("CRSDepTime", "CRSArrTime")
      
      val tableCountMissingValues = dfWithoutCancelled.select(dfWithoutCancelled.columns.map(c => sum(col(c).isNull.cast("int")).alias(c)): _*)
      //val dfWithoutMissingValues = dfWithoutCancelled.filter(dfWithoutCancelled("ArrDelay") !== null)
      //                                              .filter(dfWithoutCancelled("CRSElapsedTime") !== null)     
      // Missung Values: 288 CRSElapsedTime and 5654 ArrDelay 
      
      val dfOnlyWithFeaturesAndTarget=
          dfWithoutCancelled.drop((forbiddenColumns ++ excludedColumns ++ badlyTypedColumns): _*)                       
      
      val dfWithoutMissingValues = dfOnlyWithFeaturesAndTarget.na.drop
      //dfWithoutMissingValues.select(dfWithoutMissingValues.columns.map(c => sum(col(c).isNull.cast("int")).alias(c)): _*).show 
                        //.na.fill(0) This could be used to replace the nulls with 0
      dfWithoutMissingValues.withColumn("Month", dfWithoutMissingValues.col("Month").cast("Integer"))
                        .withColumn("DayOfWeek", dfWithoutMissingValues.col("DayOfWeek").cast("Integer"))
                        .withColumn("CRSElapsedTime", dfWithoutMissingValues.col("CRSElapsedTime").cast("Integer"))
                        .withColumn("ArrDelay", dfWithoutMissingValues.col("ArrDelay").cast("Integer"))
                        .withColumn("Distance", dfWithoutMissingValues.col("Distance").cast("Integer"))
                        .withColumn("TaxiOut", dfWithoutMissingValues.col("TaxiOut").cast("Integer"))
                 

    //val features = dfWithoutCancelled.columns.filterNot(_== "ArrDelay")

    }

    def doubleValues(sparkSesh: SparkSession, dataset: Dataset[Row], column: String): DataFrame = {
        import sparkSesh.implicits._
        dataset.select(column).map(row => normalizeTime(row.getString(0))).toDF()
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