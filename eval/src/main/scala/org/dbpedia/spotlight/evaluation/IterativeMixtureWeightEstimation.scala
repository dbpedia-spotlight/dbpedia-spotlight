package org.dbpedia.spotlight.evaluation

import io.Source
import org.dbpedia.spotlight.model.{Text, SurfaceForm, DBpediaResource, DBpediaResourceOccurrence}
import java.io.File
import org.dbpedia.spotlight.disambiguate.mixtures.{LinearRegressionMixture, Fader2Mixture, FaderMixture, Mixture}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 28.02.11
 * Time: 18:13
 * To change this template use File | Settings | File Templates.
 */

object IterativeMixtureWeightEstimation
{
    val INTERVALS = 10

    val contextWeights = (0 to INTERVALS).map(_.toDouble/INTERVALS)

    class Candidate(val occ: DBpediaResourceOccurrence,
                    val spotlightAnswer: DBpediaResource,
                    val diambiguator: String,
                    val uriCount: Int,
                    val prior: Double,
                    val score: Double,
                    val percentageOfSecond: Double,
                    val ambiguity: Int,
                    val spotProb: Double) {
        val correct = occ.resource equals spotlightAnswer

        var mixedScore = -1.0  // to be set later on

        def this(surfaceForm: String,
                 correctURI: String,
                 spotlightURI: String,
                 disambiguator: String,
                 uriCount: String,
                 prior: String,
                 score: String,
                 percentageOfSecond: String,
                 ambiguity: String,
                 spotProb: String) = {
            this(new DBpediaResourceOccurrence(new DBpediaResource(correctURI), new SurfaceForm(surfaceForm), new Text(""), -1),
                 new DBpediaResource(spotlightURI),
                 disambiguator,
                 uriCount.toInt,
                 prior.toDouble,
                 score.toDouble,
                 percentageOfSecond.toDouble,
                 ambiguity.toInt,
                 spotProb.toDouble
            )
        }

    }


    def updateWeightArray(candidates: List[Candidate], mixtureMethod: Double => Mixture, resultArray: Array[Int]) {
        for((weight,i) <- contextWeights zip (0 to INTERVALS)) {

            val mixture = mixtureMethod(weight)

            candidates.foreach(c => c.mixedScore = mixture.getScore(c.score, c.uriCount))

            val reRankedCandidates = candidates.sortBy(_.mixedScore).reverse
            if(reRankedCandidates.head.correct) {
                resultArray(i) += 1
            }

        }

    }

    def processLog(logFile: File, mixtureMethod: Double => Mixture): (Mixture, Double) = {
        var lastId = ""
        var candidates = List[Candidate]()
        var occCounter = 0

        val resultArray = new Array[Int](contextWeights.length)  //number of corrects with each weight

        System.err.println("all-prior  "+contextWeights.mkString(" | ")+"  all-context")

        for(line <- Source.fromFile(logFile, "UTF-8").getLines if !line.startsWith("occId\t")) {
            val s = line.split("\t")

            val occId = s(0)
            val disambAccuracy = s(1)
            val surfaceForm = s(2)
            val correctURI = s(3)
            val spotlightURI = s(4)
            val disambiguator = s(5)
            val uriCount = s(6)
            val prior = s(7)
            val score = s(8)
            val percentageOfSecond = s(9)
            val ambiguity = s(10)
            val spotProb = s(11)

            if(occId != lastId && lastId != "") {
                updateWeightArray(candidates, mixtureMethod, resultArray)
                candidates = Nil

                occCounter += 1
                if(occCounter%50000 == 0) {
                    printWeightPerformance(resultArray, occCounter)
                }
            }

            candidates ::= new Candidate(surfaceForm, correctURI, spotlightURI, disambiguator, uriCount, prior, score, percentageOfSecond, ambiguity, spotProb)
            lastId = occId
        }
        updateWeightArray(candidates, mixtureMethod, resultArray)

        val (bestWeight, maxAccuracy) = getBest(resultArray, occCounter)
        printWeightPerformance(resultArray, occCounter)

        (mixtureMethod(bestWeight), maxAccuracy)
    }


    private def getBest(resultArray: Array[Int], occCounter: Int): (Double, Double) = {
        var (bestWeight, maxAcc) = (0.0, 0.0)
        for((acc,i) <- resultArray.map(c => c/occCounter.toDouble) zip (0 to resultArray.length-1)) {
            if(acc > maxAcc) {
                bestWeight = i/(contextWeights.length.toDouble-1)
                maxAcc = acc
            }
        }
        (bestWeight, maxAcc)
    }

    private def printWeightPerformance(resultArray: Array[Int], occCounter: Int) {
        //val (bestWeight, bestAccuracy) = getBest(resultArray, occCounter)
        val accuracyArray = resultArray.map(c => c/occCounter.toDouble*100)
        val table = accuracyArray.map("%.2f".format(_)).mkString("all-prior ", "|", " all-context")
        System.err.println(table+"  (saw "+occCounter+" occurrences)")
    }



    def main(args: Array[String]) {
        val testLogFile = new File(args(0))

        val OCC_NUMBER = 69772256
        val SURROGATES_NUMBER = 7320772
        val RESOURCE_NUMBER = 3202919

        val mixtureMethods: List[Double => Mixture] = List(
            //new OnlyContextMixture(_),
            //new OnlyPriorMixture(_, OCC_NUMBER),
            //new SimpleLinearMixture(_, OCC_NUMBER),
            //new Fader2Mixture(_, 100000)      // 0.4 is best
            //new Fader2Mixture(_, 50000),       // 0.5 is best
//            new Fader2Mixture(_, 25000),
//            new Fader2Mixture(_, 10000),
//            new Fader2Mixture(_, 5000),
//            new Fader2Mixture(_, 2500),       // 0.9 is best
//            new Fader2Mixture(_, 1000),
//            new Fader2Mixture(_, 500),
//            new Fader2Mixture(_, 250)
             //new FaderMixture(_, 15, SURROGATES_NUMBER)
             //new LinearRegressionMixture()
        )

        System.err.println("Testing with "+mixtureMethods.length+" mixture methods...")

        var results = List[(Mixture,Double)]()
        for(mixtureMethod <- mixtureMethods) {
            System.err.println(mixtureMethod(0.5).getClass+"...")

            val (bestMixture, maxAccuracy) = processLog(testLogFile, mixtureMethod)
            System.err.println("  best: %.2f %s\n".format(maxAccuracy*100, bestMixture))

            results ::= (bestMixture, maxAccuracy)
        }

        val (bestMixture, maxAccuracy) = results.sortBy(_._2).reverse.head
        System.err.println("Best Mixture: %s with %.2f percent accuracy".format(bestMixture, maxAccuracy*100))
    }

}