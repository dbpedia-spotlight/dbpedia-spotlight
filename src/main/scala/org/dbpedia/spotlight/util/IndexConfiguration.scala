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

package org.dbpedia.spotlight.util

import org.apache.commons.logging.LogFactory
import java.util.Properties
import io.Source
import java.io.{PrintStream, FileOutputStream, FileInputStream, File}
import scala.collection.JavaConversions._
import org.apache.lucene.search.{DefaultSimilarity, Similarity}
import org.dbpedia.spotlight.lucene.similarity.InvCandFreqSimilarity
import org.apache.lucene.misc.SweetSpotSimilarity
import org.apache.lucene.analysis.snowball.SnowballAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.{Analyzer, StopAnalyzer}

/**
 * Class that holds configurations of the project.
 *
 * Defaults are not defined. All classes asking for properties have to take care
 * of the case that a property is not specified in the configuration file.
 */

//TODO: validate configuration file

object IndexConfiguration
{
    private val LOG = LogFactory.getLog(this.getClass)

    var configFileName = "spotlight.config" //TODO add this to constructor, call from main method, fail early if can't find.

    private val properties : Properties = new Properties()

    // load properties as soon as they are asked for for the first time
    load

    def load(configFile : File) {
        properties.load(new FileInputStream(configFile))
        configFileName = configFile.getAbsolutePath
        LOG.info("Loaded configuration file '" + configFile.getAbsolutePath + "'.")
    }

    def load() {
        load(new File(configFileName))
    }

    def save(configFile : File) {
        properties.store(new FileOutputStream(configFile), "")
        LOG.info("Saved configuration file '" + configFile.getAbsolutePath + "'.")
    }

    def save() {
        save(new File(configFileName))
    }

    def get(key : String, defaultValue : String) : String = {
        properties.getProperty(key, defaultValue)
    }

    def get(key : String) : String = {
        get(key, null)
    }

    def set(key : String, value : String) {
        properties.setProperty(key, value)
        //properties.store(new FileOutputStream(configFileName), "changed "+key+" to "+value+" in "+configFileName)

        val sb = new StringBuilder
        for(line <- Source.fromFile(configFileName, "UTF-8").getLines) {
            if(line startsWith key+" ") {
                sb.append(key+"  "+value+"\n")
            }
            else {
                sb.append(line+"\n")
            }
        }

        val out = new PrintStream(configFileName, "UTF-8")
        out.print(sb.toString)
        out.close
    }

    def getStopWords : Set[String] = {
        val f = new File(get("StopWordList", ""))
        if(f.isFile) {
            Source.fromFile(f, "UTF-8").getLines.toSet
        }
        else {
            StopAnalyzer.ENGLISH_STOP_WORDS_SET.asInstanceOf[Set[String]]
        }
    }

    def getAnalyzer(analyzerName : String) : Analyzer = {
        val stopWords = IndexConfiguration.getStopWords

        (new StandardAnalyzer(Version.LUCENE_29, stopWords) :: new SnowballAnalyzer(Version.LUCENE_29, "English", stopWords) :: Nil)
                .map(a => (a.getClass.getSimpleName, a))
                .toMap
                .get(analyzerName)
                .getOrElse(throw new IllegalArgumentException("Unknown Analyzer: "+analyzerName))
    }

    def getSimilarity(similarityName : String) : Similarity = {
        (new InvCandFreqSimilarity :: new SweetSpotSimilarity :: new DefaultSimilarity :: Nil)
                .map(sim => (sim.getClass.getSimpleName, sim))
                .toMap
                .get(similarityName)
                .getOrElse(throw new IllegalArgumentException("Unknown Similarity: "+similarityName))
    }

}