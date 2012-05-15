package org.dbpedia.spotlight.model

/**
 * @author Joachim Daiber
 *
 *
 *
 */

trait TextAnalyzer {

  def analyze(text: Text): TextAnalysis
  def name: String

}
