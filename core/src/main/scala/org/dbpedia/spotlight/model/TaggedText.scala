package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.tagging.TaggedTokenProvider

/**
 * @author Joachim Daiber
 */

class TaggedText(text : String, var taggedTokenProvider : TaggedTokenProvider) extends Text(text) {

  //Initialize the Tagged Text by pos-tagging it with the current TaggedTokenProvider
  taggedTokenProvider.initialize(text)

  override def equals(that : Any) =
  {
    that.isInstanceOf[Text] &&
    text.equals(that.asInstanceOf[Text].text)
  }
}