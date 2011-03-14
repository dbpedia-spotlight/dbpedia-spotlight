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
import org.dbpedia.spotlight.model.{DBpediaResource, DBpediaType, DBpediaResourceOccurrence}


class TypeFilter(val dbpediaTypes : List[DBpediaType], val blacklistOrWhitelist : FilterPolicy.ListColor) extends AnnotationFilter  {

    private val LOG = LogFactory.getLog(this.getClass)

    private val acceptable = blacklistOrWhitelist match {
        case FilterPolicy.Whitelist => (resource : DBpediaResource) =>
            resource.types.filter(given => {
            dbpediaTypes.find(listed => given equals listed) != None }
        ).nonEmpty
        case FilterPolicy.Blacklist => (resource : DBpediaResource) =>
            resource.types.filter(given => {
            dbpediaTypes.find(listed => given equals listed) != None }
        ).isEmpty
    }

    private val showUntyped = dbpediaTypes.find(t => DBpediaType.UNKNOWN equals t) != None

    def filter(occs : List[DBpediaResourceOccurrence]) : List[DBpediaResourceOccurrence] = {
        if (dbpediaTypes.filter(_.name.trim.nonEmpty).isEmpty) {
            LOG.info("types are empty: showing all types")
            return occs
        }

        LOG.info("Type filtering with "+blacklistOrWhitelist+" policy; list="+dbpediaTypes.map(_.name).mkString("List(", ",", ")"))

        occs.filter(isOk)
    }

    private def isOk(occ : DBpediaResourceOccurrence) : Boolean = {
        // if the resource does not have type and the targets contain "unknown": don't filter
        if (showUntyped && occ.resource.types.isEmpty) {
            return true
        }

        if (!acceptable(occ.resource)) {
            LOG.info("filtered out by type: "+occ.resource)
            return false
        }

        true
    }

}