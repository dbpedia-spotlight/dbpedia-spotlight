package org.dbpedia.spotlight.model

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class Token(val id: Int, val name: String, val count: Int) {

  def toString = "%s (%d)".format(name, count)

}
