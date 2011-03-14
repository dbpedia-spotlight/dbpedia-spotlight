/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.filter.annotations

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.exceptions.InputException


class ConfidenceFilter(val simThresholds : List[Double], val confidence : Double) extends AnnotationFilter  {

    private val LOG = LogFactory.getLog(this.getClass)

    def filter(occs : List[DBpediaResourceOccurrence]) : List[DBpediaResourceOccurrence] = {
        if (0 > confidence || confidence > 1) {
            throw new InputException("confidence must be between 0 and 1; got "+confidence)
        }

        val squaredConfidence = confidence*confidence
        val simThreshold = simThresholds(math.max(((simThresholds.length-1)*confidence).round.toInt, 0))

        occs.filter(isOk(_, simThreshold, squaredConfidence))
    }

    private def isOk(occ : DBpediaResourceOccurrence, simThreshold : Double, squaredConfidence : Double) : Boolean = {
        if (occ.similarityScore < simThreshold) {
            LOG.info("filtered out by similarity score threshold (%.3f<%.3f): %s".format(occ.similarityScore, simThreshold, occ))
            return false
        }
        if (occ.percentageOfSecondRank > (1-squaredConfidence)) {
            LOG.info("filtered out by threshold of second ranked percentage (%.3f>%.3f): %s".format(occ.percentageOfSecondRank, 1-squaredConfidence, occ))
            return false
        }

        true
    }

}