/*
 * Copyright 2012 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.filter.annotations

import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{DBpediaResource, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.sparql.SparqlQueryExecuter
import scalaj.collection.Imports._


class SparqlFilter(val executer : SparqlQueryExecuter, val sparqlQuery: String, val listColor : FilterPolicy.ListColor) extends AnnotationFilter  {

    private val LOG = LogFactory.getLog(this.getClass)

    val uriSet =
        if(sparqlQuery != null && sparqlQuery != "") {
            val s = executer.query(sparqlQuery).asScala.map( r => r.uri ).toSet
            LOG.debug("SPARQL "+listColor+":"+s)
            s
        }
        else {
            Set[String]()
        }

    private val acceptable = listColor match {
        case FilterPolicy.Whitelist => (resource : DBpediaResource) =>  uriSet.contains(resource.uri)
        case FilterPolicy.Blacklist => (resource : DBpediaResource) => !uriSet.contains(resource.uri)
    }

    override def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
        if(sparqlQuery == null || sparqlQuery == "") {
            Some(occ)
        }
        else if(acceptable(occ.resource)) {
            Some(occ)
        }
        else {
            LOG.info("filtered out by SPARQL "+listColor+": "+occ.resource)
            None
        }
    }

}