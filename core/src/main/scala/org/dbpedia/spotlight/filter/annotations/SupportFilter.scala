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
import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.filter.visitor.{FilterOccsVisitor, FilterElement}
import java.util
import scala.collection.JavaConversions._


class SupportFilter(val targetSupport : Int) extends AnnotationFilter with FilterElement  {

    override def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
        if (occ.resource.support > targetSupport) {
            Some(occ)
        }
        else{
            SpotlightLog.info(this.getClass, "filtered out by support (%d<%d): %s", occ.resource.support, targetSupport, occ)
            None
        }
    }

  def accept(visitor: FilterOccsVisitor, occs: util.List[DBpediaResourceOccurrence]): java.util.List[DBpediaResourceOccurrence]= {
    visitor.visit(this, occs)
  }
}