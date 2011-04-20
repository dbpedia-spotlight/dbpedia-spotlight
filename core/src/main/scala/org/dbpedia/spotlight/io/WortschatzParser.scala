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

package org.dbpedia.spotlight.io

import com.officedepot.cdap2.collection.CompactHashSet
import io.Source
import org.apache.log4j.Logger

/**
 * Hacky but easy to generalize parser for Wortschatz dataset. Up to now only used to get common words.
 * @author pablomendes
 */

object WortschatzParser {

    val LOG = Logger.getLogger(this.getClass)

    def parse(filename: String) : CompactHashSet[String] = {
        parse(filename, count => true);
    }

    def parse(filename: String, minimumCount: Int) : CompactHashSet[String] = {
        parse(filename, count => (count > minimumCount) )
    }

    def parse(filename: String, minimumCount: Int, maximumCount: Int) : CompactHashSet[String] = {
        parse(filename, count => (count > minimumCount) && (count < maximumCount))
    }

    def parse(filename: String, condition: Int => Boolean) : CompactHashSet[String] = {
        LOG.info(" parsing common words file ")
        // get lines, split in three fields, get the middle one (word)
        val commonWords = new CompactHashSet[String]();

        val log = Source.fromFile(filename, "iso-8859-1").getLines.foreach(line => {
            if (line.trim()!="") {
                val fields = line.split("\\s")
                if (condition(fields(2).toInt)) commonWords.add(fields(1))
            }
        });
        commonWords
    }
}