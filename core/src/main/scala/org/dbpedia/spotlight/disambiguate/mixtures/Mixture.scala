package org.dbpedia.spotlight.disambiguate.mixtures

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 08.03.11
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */

abstract class Mixture(val contextWeight: Double) {

    def getScore(occurrence: DBpediaResourceOccurrence) : Double

    override def toString: String

}