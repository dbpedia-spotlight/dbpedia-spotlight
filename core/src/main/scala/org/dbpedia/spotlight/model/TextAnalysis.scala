package org.dbpedia.spotlight.model

import opennlp.tools.util.Span

/**
 * @author Joachim Daiber
 */

class TextAnalysis(val sentences: Array[Span], val tokens: Array[Array[String]], val tokenPositons: Array[Array[Span]], val posTags: Array[Array[String]])
