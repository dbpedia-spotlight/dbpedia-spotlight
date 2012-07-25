package org.dbpedia.spotlight.topic.convert

import java.io.{FileWriter, PrintWriter}

/**
 * Utility class for converting vowpals prediction output to wekas arff input file
 *
 * terminal:
 * input path, output path of arff, path to categories.list from TextCorpusToInputCorpus$
 *
 * @author dirk
 */
object VowpalPredToArff {

  def main(args: Array[String]) {
    convertVWToArff("/media/Data/Wikipedia/model/lda10k/transformed/train/predictions.dat",
      "/media/Data/Wikipedia/model/lda10k/transformed/train/predictions.arff",
      "/media/Data/Wikipedia/model/lda10k/transformed/train/categories.list")
  }

  def convertVWToArff(predictionFile : String, outputFile : String, catFile : String) {
    val pw: PrintWriter = new PrintWriter(new FileWriter(outputFile))
    pw.println("@RELATION topics")

    val lines = scala.io.Source.fromFile(predictionFile).getLines()
    val categoryLines = scala.io.Source.fromFile(catFile).getLines()

    val firstLine : String = lines.next()
    val split : Array[String]= firstLine.split(" ")
    for (i <- 0 until split.length) {
        pw.println("@ATTRIBUTE topic"+i+" NUMERIC")
    }
    var cats : Array[String] = categoryLines.next().split(",")

    pw.println("@ATTRIBUTE class {"+cats.sorted.reduceLeft((acc, cat) => acc + "," + cat)+"}")
    pw.println()
    pw.println("@DATA")
    pw.println(firstLine.replaceAll(" ",",")+categoryLines.next())
    lines.foreach((fileLine) => {
          pw.println(fileLine.replaceAll(" ",",")+categoryLines.next())
    })

    pw.flush()
    pw.close()
  }

  def convertVWToNormalizedArff(predictionFile : String, outputFile : String, catFile : String) {
    val pw: PrintWriter = new PrintWriter(new FileWriter(outputFile))
    pw.println("@RELATION topics")

    val lines = scala.io.Source.fromFile(predictionFile).getLines()
    val categoryLines = scala.io.Source.fromFile(catFile).getLines()

    val firstLine : String = lines.next()
    var split : Array[String]= firstLine.split(" ")
    for (i <- 0 until split.length) {
      pw.println("@ATTRIBUTE topic"+i+" NUMERIC")
    }
    var cats : Array[String] = categoryLines.next().split(",")

    pw.println("@ATTRIBUTE class {"+cats.sorted.reduceLeft((acc, cat) => acc + "," + cat)+"}")
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
    var sum  : Double= split.foldLeft[Double](0)((acc:Double, topic:String) => acc + topic.toDouble)
    split.foreach((topic:String)=> pw.print(topic.toDouble/sum+","))
    pw.println(categoryLines.next())
    lines.foreach((fileLine) => {
      split = fileLine.split(" ")
      sum = split.foldLeft[Double](0)((acc:Double, topic:String) => acc + topic.toDouble)
      split.foreach((topic:String)=> pw.print(topic.toDouble/sum+","))
      pw.println(categoryLines.next())
    })

    pw.flush()
    pw.close()
  }
}
