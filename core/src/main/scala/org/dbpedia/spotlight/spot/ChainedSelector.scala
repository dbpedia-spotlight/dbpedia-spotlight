/*
 *  Copyright 2011 DBpedia Spotlight Developers, Scott White
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
 */

package org.dbpedia.spotlight.spot

import org.slf4j.LoggerFactory
import org.dbpedia.spotlight.model._
import scala.collection.JavaConversions._

/**
 * @author <a href="mailto:scott@onespot.com">scott white</a>
 */
class ChainedSelector(selectors : List[SpotSelector]) extends UntaggedSpotSelector {

    private val LOG = LoggerFactory.getLogger(this.getClass)

    LOG.info("Creating a chained surface form selector with %s.".format(selectors))

    def select(occurrences: java.util.List[SurfaceFormOccurrence]) : java.util.List[SurfaceFormOccurrence] = {
        var occs = occurrences
        selectors.foreach(selector =>
          occs = selector.select(occs)
        )
        occs
    }

}