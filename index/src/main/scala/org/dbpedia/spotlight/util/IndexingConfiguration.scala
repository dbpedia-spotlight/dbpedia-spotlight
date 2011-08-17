package org.dbpedia.spotlight.util

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

import org.apache.commons.logging.LogFactory
import java.util.Properties
import io.Source
import scala.collection.JavaConversions._
import org.apache.lucene.search.{DefaultSimilarity, Similarity}
import org.dbpedia.spotlight.lucene.similarity.InvCandFreqSimilarity
import org.apache.lucene.misc.SweetSpotSimilarity
import org.apache.lucene.analysis.snowball.SnowballAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.{Analyzer, StopAnalyzer}
import org.dbpedia.spotlight.exceptions.ConfigurationException
import java.io._
import collection.immutable.List._
import org.apache.lucene.analysis.standard.StandardAnalyzer

/**
 * Class that holds configuration values for indexing tasks.
 *
 * @author maxjakob
 * @author pablomendes (added getters, multi-language support)
 */

class IndexingConfiguration(val configFile: File) {

    private val LOG = LogFactory.getLog(this.getClass)

    def this(fileName: String) {
        this(new File(fileName))
    }

    var language = "English";
    var analyzer : Analyzer = new StandardAnalyzer(Version.LUCENE_29)

    private val properties : Properties = new Properties()

    LOG.info("Loading configuration file "+configFile)
    properties.load(new FileInputStream(configFile))
    validate



    def save(configFile : File) {
        properties.store(new FileOutputStream(configFile), "")
        LOG.info("Saved configuration file"+configFile)
    }

    def save() {
        save(configFile)
    }

    def get(key : String, defaultValue : String) : String = {
        properties.getProperty(key, defaultValue)
    }

    def get(key : String) : String = {
        val value = get(key, null)
        if(value == null) {
            throw new ConfigurationException(key+" not specified in "+configFile)
        }
        value
    }

    def set(key : String, value : String) {
        properties.setProperty(key, value)
        //properties.store(new FileOutputStream(configFileName), "changed "+key+" to "+value+" in "+configFileName)

        val sb = new StringBuilder
        for(line <- Source.fromFile(configFile, "UTF-8").getLines) {
            if(line startsWith key+" ") {
                sb.append(key+"  "+value+"\n")
            }
            else {
                sb.append(line+"\n")
            }
        }

        val out = new PrintStream(configFile, "UTF-8")
        out.print(sb.toString)
        out.close
    }

    def getStopWords(language: String) : Set[String] = {
        val f = new File(get("org.dbpedia.spotlight.data.stopWords."+language.toLowerCase, ""))
        try {
            Source.fromFile(f, "UTF-8").getLines.toSet
        }
        catch {
            case e: FileNotFoundException => throw new ConfigurationException("stop words file not found: "+f, e)
        }
    }

    def getAnalyzer(analyzerName : String, language: String) : Analyzer = {
        val stopWords = getStopWords(language)

        (new StandardAnalyzer(Version.LUCENE_29, stopWords) ::
         new SnowballAnalyzer(Version.LUCENE_29, language, stopWords) ::
         Nil)
            .map(a => (a.getClass.getSimpleName, a))
            .toMap
            .get(analyzerName)
            .getOrElse(throw new ConfigurationException("Unknown Analyzer: "+analyzerName))
    }

    def getSimilarity(similarityName : String) : Similarity = {
        (new InvCandFreqSimilarity :: new SweetSpotSimilarity :: new DefaultSimilarity :: Nil)
                .map(sim => (sim.getClass.getSimpleName, sim))
                .toMap
                .get(similarityName)
                .getOrElse(throw new ConfigurationException("Unknown Similarity: "+similarityName))
    }

    def getLanguage() = {
        language
    }

    def getAnalyzer = {
        analyzer
    }

    private def validate { //TODO move validation to finer grained factory classes that have specific purposes (e.g. candidate mapping, lucene indexing, etc.)

        val dumpFile = new File(get("org.dbpedia.spotlight.data.wikipediaDump"))
        if(!dumpFile.isFile) {
            throw new ConfigurationException("specified Wikipedia dump not found: "+dumpFile)
        }

        val labelsFile = new File(get("org.dbpedia.spotlight.data.labels"))
        if(!labelsFile.isFile) {
            throw new ConfigurationException("specified labels dataset not found: "+labelsFile)
        }

        val redirectsFile = new File(get("org.dbpedia.spotlight.data.redirects"))
        if(!redirectsFile.isFile) {
            throw new ConfigurationException("specified redirects dataset not found: "+redirectsFile)
        }

        val disambigFile = new File(get("org.dbpedia.spotlight.data.disambiguations"))
        if(!disambigFile.isFile) {
            throw new ConfigurationException("specified disambiguations dataset not found: "+disambigFile)
        }

        val instFile = new File(get("org.dbpedia.spotlight.data.instanceTypes"))
        if(!instFile.isFile) {
            throw new ConfigurationException("specified instance types dataset not found: "+instFile)
        }

        language = get("org.dbpedia.spotlight.language")
        if(language==null || language.size==0) {
            throw new ConfigurationException("Parameter org.dbpedia.spotlight.language not specified in config")
        }

        val stopwordsFile = new File(get("org.dbpedia.spotlight.data.stopWords."+language.toLowerCase))
        if(!stopwordsFile.isFile) {
            throw new ConfigurationException("specified stop words file not found: "+stopwordsFile)
        }

        val analyzerName = get("org.dbpedia.spotlight.lucene.analyzer")
        if(analyzerName==null) {
            throw new ConfigurationException("Analyzer not specified")
        } else {
            analyzer = getAnalyzer(analyzerName, language)
        }

    }

}