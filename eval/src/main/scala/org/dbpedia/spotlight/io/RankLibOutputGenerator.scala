package org.dbpedia.spotlight.io

import java.io.PrintWriter

import org.dbpedia.spotlight.model.DisambiguationResult

/**
 * Created by dowling on 22/07/15.
 */
class RankLibOutputGenerator(val output: PrintWriter) extends OutputGenerator{
  var qid = 0

  override def write(result: DisambiguationResult): Unit = {
    qid += 1
    var rank = 0


    // skip the sample if the correct entity is not among predictions
    if (result.predictedOccurrences.map{occ => occ.resource}.exists(res => res.getFullUri equals result.correctOccurrence.resource.getFullUri)) {
      result.predictedOccurrences.foreach { occ =>

        if (occ.resource.getFullUri equals result.correctOccurrence.resource.getFullUri)
          rank = 2
        else
          rank = 1

        val out = "%s qid:%s 1:%s 2:%s 3:%s".format(
          rank,
          qid,
          occ.featureValue[Double]("P(s|e)").get,
          occ.featureValue[Double]("P(c|e)").get,
          occ.featureValue[Double]("P(e)").get
        )
        println("Writing "+out)
        output.println(out)

      }
    }else{
      try {
        println("Resource %s not found in predictions (%s)!".format(result.correctOccurrence.resource.getFullUri.toString, result.predictedOccurrences.map(_.resource.getFullUri.toString).reduce(_ + ", " + _)))
      }catch {
        case _ : Throwable => println("Blah")
      }
    }

  }

  override def summary(summaryString: String) {
    println(summaryString)
  }

  override def flush(): Unit = {
    output.flush()
  }

  override def close(): Unit = {
    output.close()
  }
}
