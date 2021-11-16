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
    
    val count = sc.parallelize(1 to 100).filter { _ =>
        val x = math.random
        val y = math.random
        x*x + y*y < 1
        }.count()
    
    println(s"Number of numbers: $count")
 }
}
