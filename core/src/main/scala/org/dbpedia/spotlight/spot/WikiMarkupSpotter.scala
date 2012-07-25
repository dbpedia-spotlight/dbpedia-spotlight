/*
 * Copyright 2011 Pablo Mendes, Max Jakob
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

package org.dbpedia.spotlight.spot

import org.dbpedia.spotlight.model.SurfaceFormOccurrence
import org.dbpedia.spotlight.model.Text
import org.dbpedia.spotlight.string.ParseSurfaceFormText

/**
 * This class wraps a wiki text parser as a spotter.
 * It can be used for when the (human) user is acting as a spotter.
 * For example, when evaluation text is obtained with wiki markup.
 *
 * @author pablomendes
 */

class WikiMarkupSpotter extends Spotter {

    var name = "WikiMarkupSpotter"

    /**
     * Extracts a set of surface form occurrences from the text
     */
    def extract(markedText: Text): java.util.List[SurfaceFormOccurrence] = {
        ParseSurfaceFormText.parse(markedText.text)
    }

    def getName() = name
    def setName(n: String) { name = n; }

}