package upm.bd
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object MyApp {
 def main(args : Array[String]) {
    println("Hellow Rolwd")
    
   val conf = new SparkConf().setAppName("My first Spark application")
   val sc = new SparkContext(conf)
    
   val spark = SparkSession.builder().appName("Spark SQL example").config("some option", "value").enableHiveSupport() .getOrCreate()
   import spark.implicits._

   val df = spark.read.text("src/main/resources/pagecounts-20100806-030000")

   val df_with_columns = df.map(f => {
         val elements = f.getString(0).split(" ")
         (elements(0),elements(1), elements(2).toLong, elements(3).toLong)
      })

   val df_with_correct_columns = df_with_columns.withColumnRenamed("_1","project_name").withColumnRenamed("_2", "page_title").withColumnRenamed("_3", "num_requests").withColumnRenamed("_4","content_size")

   val all_projects = df_with_correct_columns.select("project_name").distinct

   val projects_starting_with_en = df_with_correct_columns.filter(col("project_name").startsWith("en"))

   val total_content_size = projects_starting_with_en.select(sum("content_size")).show

  /*  val lines = sc.textFile("src/main/resources/pagecounts-20100806-030000")
    val lineCount = lines.count
    val enPage = lines.filter(_.startsWith("en"))

    val tabularPages = enPage.map(s => s.split(' ')).map { case Array(f1,f2,f3,f4) => (f1, f2, f3.toLong, f4.toLong)} 

    val printTopFive = tabularPages.sortBy(-_._4).take(5)

    val fiveStrings = printTopFive(0) + "\n" + printTopFive(1) + "\n" + printTopFive(2) + "\n" +printTopFive(3) + "\n" + printTopFive(4)

    println(s"Amount of webpages: ${lineCount} \nTop 5 visited pages:\n"+ fiveStrings) 
    */
 }
}