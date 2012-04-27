/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dbpedia.spotlight.spot


/*
 * *
 *  * Copyright 2011 Scott White
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

import scalaj.collection.Imports._
import org.dbpedia.spotlight.model._
import scala.collection.immutable.HashSet
import io.Source
import java.io.File


/**
 * @author <a href="mailto:scott@onespot.com">scott white</a>
 */

class WhitelistSelector(dictionaryFile: File) extends SpotSelector {

    private val mentionDictionary = new HashSet[String](
      Source.fromFile(dictionaryFile).getLines()
    )

    def select(occurrences: java.util.List[SurfaceFormOccurrence]) : java.util.List[SurfaceFormOccurrence] = {
        val occs = occurrences.asScala
        occs.filter(o => mentionDictionary.contains(o.surfaceForm.name)).asJava
    }

}
