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
    if (result.predictedOccurrences.map{occ => occ.resource}.contains(result.correctOccurrence.resource)) {
      result.predictedOccurrences.foreach { occ =>

        if (occ.resource == result.correctOccurrence.resource)
          rank = 1
        else
          rank = 2

        output.println("%s qid:%s 1:%s 2:%s 3%s".format(
          rank,
          qid,
          occ.feature("P(s|e)"),
          occ.feature("P(c|e)"),
          occ.feature("P(e)")
        ))

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
