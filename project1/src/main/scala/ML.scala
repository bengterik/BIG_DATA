package flight_predictor

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.VectorIndexer
import org.apache.spark.ml.regression.{RandomForestRegressionModel, RandomForestRegressor, LinearRegression}
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Row
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.linalg.{Matrix, Vectors}
import org.apache.spark.ml.stat.Correlation
import org.apache.spark.sql.Row

case class ML(sparkSesh: SparkSession, 
         dataset: Dataset[Row], 
         target: String,
         categoricalVariables: List[String]) {
    
    def randomForest(
         trainingDataPart: Double, 
         testDataPart: Double): String = {
        import sparkSesh.implicits._
        
        val features = dataset.columns.filterNot(col => col == target || categoricalVariables.contains(col))
        
        var indexDataset = dataset

        for (category <- categoricalVariables) {
            indexDataset =
                new StringIndexer()
                    .setInputCol(category)
                    .setOutputCol("indexed" + category)
                    .fit(indexDataset).transform(dataset)
        }
        
        val indexedDF = indexDataset

        val assembler = new VectorAssembler()
            .setInputCols(features ++ categoricalVariables.map("indexed" ++ _))      
            .setOutputCol("features")

        val dfWithFeaturesVec = assembler.transform(indexedDF)

        val featureIndexer = new VectorIndexer()
            .setInputCol("features")
            .setOutputCol("indexedFeatures")
            .setMaxCategories(21)
            .fit(dfWithFeaturesVec)


        // split the data 
        val Array(trainingData, testData) = 
            dfWithFeaturesVec.randomSplit(Array(trainingDataPart,testDataPart))
        
        // train a RandomForest Model
        val randomForest = new RandomForestRegressor()
            .setLabelCol(target)
            .setFeaturesCol("indexedFeatures")
        
        // Chain indexer and forest in a Pipline
        val pipeline = new Pipeline()
            .setStages(Array(featureIndexer, randomForest))
        
        // train the model
        val model = pipeline.fit(trainingData)

        // make Predicitons
        val predictions = model.transform(testData)

        // show Examples
        println("Features: " + features.mkString(" | "))
        predictions.select("prediction", target, "features").show(3)

        val randomForestModel = model.stages(1).asInstanceOf[RandomForestRegressionModel]

        rmse(predictions)
    }

    def linearRegression(): Unit = {
        val features = dataset.columns.filterNot(_== target)
        val assembler = new VectorAssembler()
            .setInputCols(features)      
            .setOutputCol("features")
        
        val dfWithFeaturesVec = assembler.transform(dataset).drop(features: _*)

        val lr = new LinearRegression()
        .setFeaturesCol("features")
        .setLabelCol("ArrDelay")
        .setMaxIter(10)
        .setRegParam(0.3)
        .setElasticNetParam(0.8)

        // Fit the model
        val lrModel = lr.fit(dfWithFeaturesVec)

        // Print the coefficients and intercept for linear regression
        println(s"Coefficients: ${lrModel.coefficients} Intercept: ${lrModel.intercept}")


        // Summarize the model over the training set and print out some metrics
        val summary = lrModel.summary
        println(s"RMSE: ${summary.rootMeanSquaredError}")
    }

    def rmse(ds: Dataset[Row]): String = {
        val evaluator = new RegressionEvaluator()
            .setLabelCol(target)
            .setPredictionCol("prediction")
            .setMetricName("rmse") //rmse = root mean squared error
        
        
        val rmse = evaluator.evaluate(ds)

        s"Root Mean Squared Error (RMSE) on test data = $rmse"
    }

    def correlationMatrix(): Dataset[Row] = {
        import sparkSesh.implicits._
        
        val features = dataset.columns.filterNot(col => col == target || categoricalVariables.contains(col))
        
        var indexDataset = dataset

        for (category <- categoricalVariables) {
            indexDataset =
                new StringIndexer()
                    .setInputCol(category)
                    .setOutputCol("indexed" + category)
                    .fit(indexDataset).transform(dataset)
        }
        
        val indexedDF = indexDataset

        val assembler = new VectorAssembler()
            .setInputCols(features ++ categoricalVariables.map("indexed" ++ _))      
            .setOutputCol("features")

        val dfWithFeaturesVec = assembler.transform(indexedDF)
        
        Correlation.corr(dfWithFeaturesVec, "features")
    }

}