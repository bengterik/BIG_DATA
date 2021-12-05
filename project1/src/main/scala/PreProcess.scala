package flight_predictor

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.VectorAssembler
import scala.collection.mutable.ListBuffer
import org.apache.spark.sql._

/** Methods for loading and modifying dataframes to prepare for model application */
object PreProcess {

    /** Loads and formats multiple dataframes from an array of [directory, file, file, ...]
     *
     *  @param spark current spark session
     *  @param files relative path to directory followed by files 
     *  @return a unified dataset from the input files
     */
    def loadDF(spark: SparkSession, files: Array[String]): DataFrame = {
        val path = files(0)

        var dataframes: ListBuffer[DataFrame] = ListBuffer()
        for (i <- 1 until files.size) {
            dataframes = dataframes :+ loadDF(spark, path ++ files(i) ++ ".csv")
        }

        dataframes.reduce(_.union(_))
    }

    /** Formats a dataframe from a specified path
     *
     *  @param spark spark session
     *  @param path relative file path
     *  @return a formatted dataset created from the input file
     */
    def loadDF(spark: SparkSession, path: String): Dataset[Row] = {
      val df = loadFromFile(spark, path)
      removeUnwantedColumns(formatColumns(df))           
    }

    /** Casts the the specified columns to the Integer type as well as formatting CRSDeptime
     *  to integer hours.
     *
     *  @param ds dataset
     *  @return dataset with Int-casted columns and formatted time in CRSDepTime 
     */
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

    /** Loads a dataframe from a path, making the first row the headings
   *
   *  @param spark spark session
   *  @param path relative path to file
   *  @return a dataframe constructed from the provided file
   */
    def loadFromFile(spark: SparkSession, path: String): Dataset[Row] = {
        val fromFile = spark.read.csv(path)
        val headings = fromFile.take(1)(0).toSeq.map(_.toString)
        val rowToSkip = fromFile.first
        
        fromFile.filter(row => row != rowToSkip).toDF(headings:_*)
    }

    /** Removes cancelled flights as well as uwanted or forbidden columns
   *
   *  @param ds dataset 
   *  @return dataframe with removed columns 
   */
    def removeUnwantedColumns(ds: Dataset[Row]): Dataset[Row] = {
        val forbiddenColumns = List("ArrTime", "DepTime", "ActualElapsedTime", "AirTime", "TaxiIn", "Diverted", "CarrierDelay", "WeatherDelay", "NASDelay", "SecurityDelay", "LateAircraftDelay")
        val excludedColumns = List("Year", "DayOfMonth", "FlightNum", "CRSArrTime", "Cancelled", "TailNum", "DepDelay", "Cancelled", "CancellationCode", "Origin", "Dest")
        
        ds.filter(ds("Cancelled") < 1)
          .drop((forbiddenColumns ++ excludedColumns): _*)
          .na.drop
    }

    /** Returns a dataset where the specified column has been formated 
      * from standard time to integer 
      * 
      * Example showing how to turn 16.33 into 16.55 in order to get correct rounding to hours:
      *      Round ( [16.33 - floor(16.33)] * 1.6666667 + floor(16.33) ) =
      *     Round ( [16.33 - 16.00] * 1.6666667 + 16.00 ) = 16.55
      *  Also: modulo is added in the end to avoid having an hour bigger than 23. 
      *
      * @param ds dataset 
      * @param column column to format to integer hours
      * @return dataset with formatted column
      */
    
    def formatIntoHours(ds: Dataset[Row], column: String): Dataset[Row] = {
        ds.withColumn(column, 
            round((ds(column).cast("Integer") / 100 - floor(ds(column).cast("Integer") / 100)) 
            * 1.666666667 
            + floor(ds(column).cast("Integer") / 100)) % 24
            )
    }
}
