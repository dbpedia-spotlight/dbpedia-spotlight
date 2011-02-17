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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web.rest;

import org.dbpedia.spotlight.exceptions.ConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Holds all configuration parameters needed to run the DBpedia Spotlight Server
 * Reads values from a config file
 * (TODO) and should make tests to validate if the inputs are acceptable, failing gracefully and early.
 *
 * @author pablomendes
 */
public class ServerConfiguration {

    public final static String DEFAULT_TEXT = "";
    public final static String DEFAULT_CONFIDENCE = "0.5";
    public final static String DEFAULT_SUPPORT = "30";
    public final static String DEFAULT_TYPES = "";
    public final static String DEFAULT_SPARQL = "";
    public final static String DEFAULT_POLICY = "whitelist";
    public final static String DEFAULT_COREFERENCE_RESOLUTION = "true";

    protected String serverURI = "http://localhost:2222/rest/";
    protected String spotterFile    = "/home/pablo/web/TitRedDis.spotterDictionary";
    protected String indexDirectory = "/home/pablo/web/DisambigIndex.singleSFs-plusTypes.SnowballAnalyzer.DefaultSimilarity";
    protected String sparqlMainGraph = "http://dbpedia.org";
    protected String sparqlEndpoint = "http://dbpedia.org/sparql";


    public String getServerURI() {
        return serverURI;
    }

    public String getSpotterFile() {
        return spotterFile;
    }

    public String getIndexDirectory() {
        return indexDirectory;
    }

    public String getSparqlMainGraph() {
        return sparqlMainGraph;
    }

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

//final static String spotterFile= "/home/pablo/web/dbpedia36data/2.9.3/surface_forms-Wikipedia-TitRedDis.thresh3.spotterDictionary";
    //final static String indexDirectory = "/home/pablo/web/dbpedia36data/2.9.3/Index.wikipediaTraining.Merged.SnowballAnalyzer.DefaultSimilarity";

    public ServerConfiguration(String fileName) throws ConfigurationException {
        //read config properties
        Properties config = new Properties();
        try {
            config.load(new FileInputStream(new File(fileName)));
        } catch (IOException e) {
            throw new ConfigurationException("Cannot find configuration file.",e);
        }
        //set spotterFile, indexDir...
        /*
        jcs.default.cacheattributes.MaxObjects = 5000
         */
        indexDirectory = config.getProperty("org.dbpedia.spotlight.index.dir");
        spotterFile = config.getProperty("org.dbpedia.spotlight.spot.dictionary");
        serverURI = config.getProperty("org.dbpedia.spotlight.web.rest.uri");
        if (!serverURI.endsWith("/")) serverURI = serverURI.concat("/");
        //...

    }


}
