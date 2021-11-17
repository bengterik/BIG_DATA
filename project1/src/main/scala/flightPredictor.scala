import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object MyApp {
 def main(args : Array[String]) {
    
    println("Hellow Rolwd")
    Logger.getLogger("org").setLevel(Level.WARN)
    
    val conf = new SparkConf().setAppName("My first Spark application")
    val sc = new SparkContext(conf)
    val spark = SparkSession.builder().appName("Spark SQL project").config("some option", "value").enableHiveSupport().getOrCreate()
    import spark.implicits._


    // Loading the file into DF and formatting

    val fromFile = spark.read.csv("src/main/resources/2008.csv")
    val rowToSkip = fromFile.first
    val headings = fromFile.take(1)(0).toSeq.map(_.toString)
    val df = fromFile.filter(row => row != rowToSkip).toDF(headings:_*)

    df.show(3)
 }
}
