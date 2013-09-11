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

import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.model.{OntologyType, DBpediaResource, DBpediaType, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.filter.visitor.{FilterElement, FilterOccsVisitor}
import scala.collection.JavaConversions._
import java.util

class TypeFilter(var ontologyTypes : List[OntologyType], val blacklistOrWhitelist : FilterPolicy.ListColor) extends AnnotationFilter  with FilterElement {

    if (ontologyTypes==null)
        ontologyTypes = List[OntologyType]()
    else
        ontologyTypes = ontologyTypes.filter(_.typeID.trim.nonEmpty)

    if(ontologyTypes.isEmpty) SpotlightLog.info(this.getClass, "types are empty: showing all types")  // see comment below


    private val acceptable = blacklistOrWhitelist match {
        case FilterPolicy.Whitelist => (resource : DBpediaResource) =>
            resource.types.filter(given => ontologyTypes.find(listed => given equals listed) != None).nonEmpty
        case FilterPolicy.Blacklist => (resource : DBpediaResource) =>
            resource.types.filter(given => ontologyTypes.find(listed => given equals listed) != None).isEmpty
    }

    private val showUntyped = ontologyTypes.find(t => DBpediaType.UNKNOWN equals t) != None

    override def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
        if(ontologyTypes.isEmpty) {   // hack, because web demo does not guarantee to check all types when loading!
            Some(occ)
        }
        else if(showUntyped && occ.resource.types.isEmpty) {
            Some(occ)
        }
        else if(acceptable(occ.resource)) {
            SpotlightLog.debug(this.getClass, "Acceptable! %s", occ.resource)
            Some(occ)
        }
        else {
            SpotlightLog.info(this.getClass, "filtered out by type %s: %s list=%s", blacklistOrWhitelist, occ.resource, ontologyTypes.map(_.typeID).mkString("List(", ",", ")"))
            None
        }
    }


  def accept(visitor: FilterOccsVisitor, occs: util.List[DBpediaResourceOccurrence]): java.util.List[DBpediaResourceOccurrence]= {
    visitor.visit(this, occs)
  }

}