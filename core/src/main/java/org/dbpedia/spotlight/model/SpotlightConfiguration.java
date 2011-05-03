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

package org.dbpedia.spotlight.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jcs.utils.threadpool.ThreadPoolManager;
import org.dbpedia.spotlight.exceptions.ConfigurationException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Holds all configuration parameters needed to run the DBpedia Spotlight Server
 * Reads values from a config file
 * (TODO) and should make tests to validate if the inputs are acceptable, failing gracefully and early.
 *
 * @author pablomendes
 */
public class SpotlightConfiguration {

    private static Log LOG = LogFactory.getLog(SpotlightConfiguration.class);

    public final static String DEFAULT_TEXT = "";
    public final static String DEFAULT_CONFIDENCE = "0.5";
    public final static String DEFAULT_SUPPORT = "30";
    public final static String DEFAULT_TYPES = "";
    public final static String DEFAULT_SPARQL = "";
    public final static String DEFAULT_POLICY = "whitelist";
    public final static String DEFAULT_COREFERENCE_RESOLUTION = "true";

    protected String spotterFile    = "";
    protected String indexDirectory = "";
    protected List<Double> similarityThresholds;
    protected String similarityThresholdsFile = "similarity-thresholds.txt";

    protected String serverURI       = "http://localhost:2222/rest/";
    protected String sparqlMainGraph = "http://dbpedia.org/sparql";
    protected String sparqlEndpoint  = "http://dbpedia.org";


    public String getServerURI() {
        return serverURI;
    }

    public String getSpotterFile() {
        return spotterFile;
    }

    public String getIndexDirectory() {
        return indexDirectory;
    }

    public List<Double> getSimilarityThresholds() {
        return similarityThresholds;
    }

    public String getSparqlMainGraph() {
        return sparqlMainGraph;
    }

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

//final static String spotterFile= "/home/pablo/web/dbpedia36data/2.9.3/surface_forms-Wikipedia-TitRedDis.thresh3.spotterDictionary";
    //final static String indexDirectory = "/home/pablo/web/dbpedia36data/2.9.3/Index.wikipediaTraining.Merged.SnowballAnalyzer.DefaultSimilarity";

    public SpotlightConfiguration(String fileName) throws ConfigurationException {
        //read config properties
        Properties config = new Properties();
        try {
            config.load(new FileInputStream(new File(fileName)));
        } catch (IOException e) {
            throw new ConfigurationException("Cannot find configuration file "+fileName,e);
        }
        //set spotterFile, indexDir...
        /*
        jcs.default.cacheattributes.MaxObjects = 5000
         */
        indexDirectory = config.getProperty("org.dbpedia.spotlight.index.dir").trim();
        if(!new File(indexDirectory).isDirectory()) {
            throw new ConfigurationException("Cannot find index directory "+indexDirectory);
        }

        try {
            BufferedReader r = new BufferedReader(new FileReader(new File(indexDirectory, similarityThresholdsFile)));
            String line;
            similarityThresholds = new ArrayList<Double>();
            while((line = r.readLine()) != null) {
                similarityThresholds.add(Double.parseDouble(line));
            }
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Similarity threshold file '"+similarityThresholdsFile+"' not found in index directory "+indexDirectory,e);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Error parsing similarity value in '"+indexDirectory+"/"+similarityThresholdsFile,e);
        } catch (IOException e) {
            throw new ConfigurationException("Error reading '"+indexDirectory+"/"+similarityThresholdsFile,e);
        }

        spotterFile = config.getProperty("org.dbpedia.spotlight.spot.dictionary").trim();
        if(!new File(spotterFile).isFile()) {
            throw new ConfigurationException("Cannot find spotter file "+spotterFile);
        }

        serverURI = config.getProperty("org.dbpedia.spotlight.web.rest.uri").trim();
        if (!serverURI.endsWith("/")) {
            serverURI = serverURI.concat("/");
        }
        try {
            new URI(serverURI);
        } catch (URISyntaxException e) {
            throw new ConfigurationException("Server URI not valid.",e);
        }

        sparqlEndpoint = config.getProperty("org.dbpedia.spotlight.sparql.endpoint").trim(); //TODO how to fail gracefully for endpoint?
        sparqlMainGraph = config.getProperty("org.dbpedia.spotlight.sparql.graph").trim();

        //...

    }


}
