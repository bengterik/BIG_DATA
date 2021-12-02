package flight_predictor
/*import org.apache.spark.ml.feature.VectorAssembler
import spark.implicits._
import org.apache.parquet.filter2.predicate.Operators.Column
import org.apache.spark.ml.feature.Normalizer
import org.apache.spark.ml.regression.LinearRegression

object ML {
    def mlModel(sparkSesh: SparkSession, dataset: Dataset[Row], features: List[String], target: String): DataFrame = {
        val assembler = new VectorAssembler()
            .setInputCols(features)      
            .setOutputCol("features")
        val output = assembler.transform(df)
        //output.show(truncate = false)
        val normalizer = new Normalizer()
            .setInputCol("features")
            .setOutputCol("normFeatures")
            .setP(1.0)
        val l1NormData = normalizer.transform(output)
        //l1NormData.show(truncate=false)          

        val linReg = new LinearRegression()
            .setFeaturesCol("features")
            .setLabelCol(target)
            .setMaxIter()                                                                                                                                                                                                                                                                                                                                                                                                                         
    }
}                                                                       
*/
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.VectorIndexer
import org.apache.spark.ml.regression.{RandomForestRegressionModel, RandomForestRegressor}
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Row
import org.apache.spark.ml.feature.VectorAssembler

object ML {
    def mlModel(
    sparkSesh: SparkSession, dataset: Dataset[Row], 
    target: String, trainingDataPart: Double, testDataPart: Double): String 
    = {
        import sparkSesh.implicits._
        
        val features = dataset.columns.filterNot(_== target)
        val assembler = new VectorAssembler()
            .setInputCols(features)      
            .setOutputCol("features")

        val dfWithFeaturesVec = assembler.transform(dataset)
        val featureIndexer = new VectorIndexer()
            .setInputCol("features")
            .setOutputCol("indexedFeatures")
            .setMaxCategories(4)
            .fit(dfWithFeaturesVec)
        // split the data 
        val Array(trainingData, testData) = 
            dfWithFeaturesVec.randomSplit(Array(trainingDataPart,testDataPart))
        
        // train a RandomForest Model
        val randomForest = new RandomForestRegressor()
            .setLabelCol(target)
            .setFeaturesCol("indexedFeatures")
        
        // Chain indexer and forest in a Pipline
        val pipline = new Pipeline()
            .setStages(Array(featureIndexer, randomForest))
        
        // train the model
        val model = pipline.fit(trainingData)

        // make Predicitons
        val predictions = model.transform(testData)

        // show Examples
        predictions.select("prediction", target, "features").show(3)

        // select (prediction, true target value) and compute the test error.
        val evaluator = new RegressionEvaluator()
            .setLabelCol(target)
            .setPredictionCol("prediction")
            .setMetricName("rmse") //rmse = root mean squared error
        val rmse = evaluator.evaluate(predictions)

        val randomForestModel = model.stages(1).asInstanceOf[RandomForestRegressionModel]
        
        s"Root Mean Squared Error (RMSE) on test data = $rmse" ++ s"Learned regression forest model:\n ${randomForestModel.toDebugString}"
    }
}