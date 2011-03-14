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

import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{DBpediaResource, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.sparql.SparqlQueryExecuter
import scala.collection.JavaConversions._


class SparqlFilter(val executer : SparqlQueryExecuter, val sparqlQuery: String, val blacklistOrWhitelist : FilterPolicy.ListColor) extends AnnotationFilter  {

    private val LOG = LogFactory.getLog(this.getClass)

    def filter(occs : List[DBpediaResourceOccurrence]) : List[DBpediaResourceOccurrence] = {
        if(sparqlQuery == null || sparqlQuery == "") {
            return occs
        }

        val uriSet = executer.query(sparqlQuery).toSet
        LOG.debug("SPARQL "+blacklistOrWhitelist+":"+uriSet);

        val acceptable = blacklistOrWhitelist match {
            case FilterPolicy.Whitelist => (resource : DBpediaResource) =>  uriSet.contains(resource.uri)
            case FilterPolicy.Blacklist => (resource : DBpediaResource) => !uriSet.contains(resource.uri)
        }

        occs.filter(isOk(_, acceptable))
    }

    private def isOk(occ : DBpediaResourceOccurrence, acceptable : DBpediaResource => Boolean) : Boolean = {
        if (!acceptable(occ.resource)) {
            LOG.info("filtered out by SPARQL "+blacklistOrWhitelist+": "+occ.resource)
            return false
        }

        true
    }



}