To run the spark application:
1. Download scala, spark and sbt
2. Download the flight data file
3. Put the file under src/main/resources
4. In directory project: sbt package
5. Submit using spark: spark-submit --master local target/scala-2.12/sparkapp_2.12-1.0.0.jar

Enjoy:)
