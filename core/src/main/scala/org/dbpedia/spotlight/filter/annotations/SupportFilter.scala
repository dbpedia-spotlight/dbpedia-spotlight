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


class SupportFilter(val targetSupport : Int) extends AnnotationFilter  {

    private val LOG = LogFactory.getLog(this.getClass)

    override def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
        if (occ.resource.support > targetSupport) {
            Some(occ)
        }
        else{
            LOG.info("filtered out by support ("+occ.resource.support+"<"+targetSupport+"): "+occ)
            None
        }
    }

}