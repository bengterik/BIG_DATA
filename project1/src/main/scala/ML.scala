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

/** A class wrapping the dataset, target and any categorical values.
 *
 *  @constructor sets up for model application
 *  @param spark current spark session
 *  @param dataset (dataset with unused columns removed) that the models/functions can be applied to 
 *  @param target target variable for the model
 *  @param categoricalVariables categorical variables from the model which will need indexation
 */
case class ML(spark: SparkSession, 
         dataset: Dataset[Row], 
         target: String,
         categoricalVariables: List[String]) {
    
    /** Regression using the random forest method on the dataset provided
      *
      * @param trainingDataPart share of data used for training the model
      * @param testDataPart share of data used for testing the model
      * @return RMSE of the model
      */
    def randomForest(
         trainingDataPart: Double, 
         testDataPart: Double): String = {
        import spark.implicits._
        
        val features = dataset.columns.filterNot(col => col == target || categoricalVariables.contains(col))

        val dfWithFeaturesVec = withFeaturesVector()

        val featureIndexer = new VectorIndexer()
            .setInputCol("features")
            .setOutputCol("indexedFeatures")
            .setMaxCategories(21)
            .fit(dfWithFeaturesVec)

        val Array(trainingData, testData) = 
            dfWithFeaturesVec.randomSplit(Array(trainingDataPart,testDataPart))
        
        val randomForest = new RandomForestRegressor()
            .setLabelCol(target)
            .setFeaturesCol("indexedFeatures")
        
        val pipeline = new Pipeline()
            .setStages(Array(featureIndexer, randomForest))
        
        val model = pipeline.fit(trainingData)

        val predictions = model.transform(testData)

        println("Features: " + features.mkString(" | "))
        predictions.select("prediction", target, "features").show(3)

        val randomForestModel = model.stages(1).asInstanceOf[RandomForestRegressionModel]

        rmse(predictions)
    }
    
    /** Regression using the linear regression model on the dataset provided.
      *
      * @param trainingDataPart share of data used for training the model
      * @param testDataPart share of data used for testing the model
      * @return RMSE of the model
      */
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

    /** Method to index categorical variables and assemble all features into a feature vector
    *
      * @return Dataset with feature vector
      */
    def withFeaturesVector(): Dataset[Row] = {
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

        assembler.transform(indexedDF)
    }

    /** Returns the root mean square error between "target" and "prediction" column.
      *
      * @param ds dataset
      * @return String showing the RMSE error
      */
    def rmse(ds: Dataset[Row]): String = {
        val evaluator = new RegressionEvaluator()
            .setLabelCol(target)
            .setPredictionCol("prediction")
            .setMetricName("rmse") //rmse = root mean squared error
        
        val rmse = evaluator.evaluate(ds)

        s"Root Mean Squared Error (RMSE) on test data = $rmse"
    }

    /** Regression using the Random Forest Regressor on the dataset provided
      *
      * @param path sets directory to place JSON file
      * @param name sets name of file
      * @return Writes the matrix in JSON-format to a file specified in path
      */
    def correlationMatrix(path: String, name: String): Unit = {
        val df = withFeaturesVector()
        
        val cm = Correlation.corr(df, "features")

        cm.write.json(path ++ name ++ ".json")
    }

}