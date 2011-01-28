package org.dbpedia.spotlight.evaluation

object EvalParams {
    val confidenceInterval = List(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.6, 0.7, 0.8, 0.9); //TODO , 0.99);
    val supportInterval = List(0,50,100,150,250,350,500,750,1000);
    val spotProbInterval = List(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.6, 0.7, 0.8, 0.9);


}