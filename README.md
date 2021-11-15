To run the spark application:
1. Download scala, spark and sbt
2. Download the pagecounts file https://dumps.wikimedia.org/other/pagecounts-raw/2010/2010-08/pagecounts-20100806-030000.gz
3. Put it under src/main/resources
4. Submit using spark: spark-submit --master local target/scala-2.12/sparkapp_2.12-1.0.0.jar

Enjoy:)
