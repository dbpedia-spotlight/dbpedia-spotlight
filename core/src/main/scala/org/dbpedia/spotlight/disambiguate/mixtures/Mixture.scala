package org.dbpedia.spotlight.disambiguate.mixtures

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 08.03.11
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */

abstract class Mixture(val contextWeight: Double) {

    if(0 > contextWeight || contextWeight > 1) {
        throw new IllegalArgumentException("context weight must be between 0 and 1; got "+contextWeight)
    }

    def getScore(contextScore: Double, uriCount: Int) : Double

    override def toString: String

}