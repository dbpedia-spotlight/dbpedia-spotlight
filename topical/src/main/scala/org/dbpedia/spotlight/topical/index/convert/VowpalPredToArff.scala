package org.dbpedia.spotlight.topical.convert

import java.io.{File, FileWriter, PrintWriter}

/**
 * Utility class for converting vowpals prediction output to wekas arff input file
 * <br>
 * terminal:  <br>
 * input of vowpal prediction, topics.list that belonged to the training corpus from which predictions were computed, output path of arff
 *
 * @author dirk
 */
object VowpalPredToArff {

    def main(args: Array[String]) {
        convertVWToArff(new File(args(0)), new File(args(1)), new File(args(2)))
    }

    def convertVWToArff(predictionFile: File, topicsFile: File, outputFile: File) {
        val pw: PrintWriter = new PrintWriter(new FileWriter(outputFile))
        pw.println("@RELATION topics")

        val lines = scala.io.Source.fromFile(predictionFile).getLines()
        val categoryLines = scala.io.Source.fromFile(topicsFile).getLines()

        val firstLine: String = lines.next()
        val split: Array[String] = firstLine.split(" ")
        for (i <- 0 until split.length) {
            pw.println("@ATTRIBUTE topic" + i + " NUMERIC")
        }
        var cats: Array[String] = categoryLines.next().split(",")

        pw.println("@ATTRIBUTE class {" + cats.sorted.reduceLeft((acc, cat) => acc + "," + cat) + "}")
        pw.println()
        pw.println("@DATA")
        pw.println(firstLine.replaceAll(" ", ",") + categoryLines.next())
        lines.foreach((fileLine) => {
            pw.println(fileLine.replaceAll(" ", ",") + categoryLines.next())
        })

        pw.flush()
        pw.close()
    }

    def convertVWToNormalizedArff(predictionFile: File, outputFile: File, catFile: File) {
        val pw: PrintWriter = new PrintWriter(new FileWriter(outputFile))
        pw.println("@RELATION topics")

        val lines = scala.io.Source.fromFile(predictionFile).getLines()
        val categoryLines = scala.io.Source.fromFile(catFile).getLines()

        val firstLine: String = lines.next()
        var split: Array[String] = firstLine.split(" ")
        for (i <- 0 until split.length) {
            pw.println("@ATTRIBUTE topic" + i + " NUMERIC")
        }
        var cats: Array[String] = categoryLines.next().split(",")

        pw.println("@ATTRIBUTE class {" + cats.sorted.reduceLeft((acc, cat) => acc + "," + cat) + "}")
        /*pw.print("@ATTRIBUTE class { ")
        for (i<- 0 until numberOfCategories) {
          pw.print(i)
          if (i < numberOfCategories-1)
            pw.print(",")
          else
            pw.print("}")
        }*/

        //TODO use to vowpalprediterator
        pw.println()
        pw.println("@DATA")
        split = firstLine.split(" ")
        var sum: Double = split.foldLeft[Double](0)((acc: Double, topic: String) => acc + topic.toDouble)
        split.foreach((topic: String) => pw.print(topic.toDouble / sum + ","))
        pw.println(categoryLines.next())
        lines.foreach((fileLine) => {
            split = fileLine.split(" ")
            sum = split.foldLeft[Double](0)((acc: Double, topic: String) => acc + topic.toDouble)
            split.foreach((topic: String) => pw.print(topic.toDouble / sum + ","))
            pw.println(categoryLines.next())
        })

        pw.flush()
        pw.close()
    }
}
