package org.dbpedia.spotlight.disambiguate.mixtures

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 08.03.11
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */

abstract class Mixture(val contextWeight: Double) {

    def getScore(contextScore: Double, uriCount: Int) : Double

    override def toString: String

}