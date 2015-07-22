package org.dbpedia.spotlight.io

import java.io.PrintWriter

import org.dbpedia.spotlight.model.DisambiguationResult

/**
 * Created by dowling on 22/07/15.
 */
class RankLibOutputGenerator(val output: PrintWriter) extends OutputGenerator{
  override def write(result: DisambiguationResult): Unit = {
    result.predictedOccurrences.map{key => key.feature()}
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
