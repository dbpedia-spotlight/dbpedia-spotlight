/*
 * Copyright 2011 DBpedia Spotlight Development Team
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

package org.dbpedia.spotlight.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.spot.CoOccurrenceBasedSelector;
import org.dbpedia.spotlight.spot.NESpotter;
import org.dbpedia.spotlight.spot.OpenNLPUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Configuration for Spotter and Spot Selection.
 *
 * @author Joachim Daiber
 * @author pablomendes - added spotter policies
 */
public class SpotterConfiguration {

    private static Log LOG = LogFactory.getLog(SpotterConfiguration.class);

    public final String PREFIX_COOCCURRENCE_SELECTOR = "org.dbpedia.spotlight.spot.cooccurrence.";

    private final String PREFIX_OPENNLP = "org.dbpedia.spotlight.spot.opennlp.";

    Properties config = new Properties();
    protected String spotterFile    = "";

    private Map<String, String> openNLPModelsURI = new HashMap<String, String>(3);

    private String  spotterSurfaceForms = "";

    public enum SpotterPolicy {Default,
        LingPipeSpotter,
        AtLeastOneNounSelector,
        CoOccurrenceBasedSelector,
        NESpotter,
        KeyphraseSpotter,
        OpenNLPChunkerSpotter,
        WikiMarkupSpotter,
        SpotXmlParser,
        AhoCorasickSpotter
    }


    public SpotterConfiguration(String fileName) throws ConfigurationException {

        //Read config properties:
        try {
            config.load(new FileInputStream(new File(fileName)));
        } catch (IOException e) {
            throw new ConfigurationException("Cannot find configuration file "+fileName,e);
        }

        // Validate spotters
        List<SpotterPolicy> spotters = getSpotterPolicies();
        LOG.info(String.format("Will load spotters: %s.",spotters));

        // Validate LingPipeSpotter
        if (spotters.contains(SpotterPolicy.LingPipeSpotter)) {
            //Load spotter configuration:
            spotterFile = config.getProperty("org.dbpedia.spotlight.spot.dictionary").trim();
            if(!new File(spotterFile).isFile()) {
                throw new ConfigurationException("Cannot find spotter file "+spotterFile);
            }
        }

        // Validate CoOccurrenceBasedSelector
        if (spotters.contains(SpotterPolicy.CoOccurrenceBasedSelector)) {

            //Check if all required parameters are there, trim whitespace
            String[] parameters = {"database.jdbcdriver", "database.connector", "database.user", "database.password",
                    "classifier.unigram", "classifier.ngram", "datasource"};

            for(String parameter : parameters) {
                try{
                    //Trim whitepsace from the parameter
                    config.setProperty(PREFIX_COOCCURRENCE_SELECTOR + parameter, config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + parameter).trim());
                }catch(NullPointerException e){
                    //One of the configuration property required for this Spot selector was not there.
                    throw new ConfigurationException(String.format("Cannot find required configuration property '%s' for co-occurrence based spot selector %s.",PREFIX_COOCCURRENCE_SELECTOR + parameter, CoOccurrenceBasedSelector.class));
                }
            }

            //Check if all the required files are there:
            String[] parameterFiles = {"classifier.unigram", "classifier.ngram"};
            for(String fileparameter : parameterFiles) {
                String file = config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + fileparameter);
                if(!new File(file).isFile()) {
                    throw new ConfigurationException("Cannot find required file '"+file+"' for configuration parameter "+PREFIX_COOCCURRENCE_SELECTOR + fileparameter);
                }
            }

            if(!config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "datasource").equalsIgnoreCase("ukwac") &&
               !config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "datasource").equalsIgnoreCase("google")) {
                throw new ConfigurationException(PREFIX_COOCCURRENCE_SELECTOR + "datasource must be one of 'ukwac' or 'google'.");
            }

            //check if database exists (if it's file)
            //jdbc:hsqldb:file:/data/spotlight/3.7/spotsel/ukwac_candidate;shutdown=true&readonly=true
            String configConnectorParam = PREFIX_COOCCURRENCE_SELECTOR + "database.connector";
            String connector = config.getProperty(configConnectorParam,"");
            try {
                if (connector.contains(":file:")) {
                    String[] parts = connector.split(":");
                    String path = parts[parts.length-1].split(";")[0];
                    if (!new File(path).exists())
                        throw new ConfigurationException(String.format("The file path %s specified for %s does not exist. Either remove CoOccurrenceBasedSelector from the list of spotters or set the correct path.",path,configConnectorParam));
                }
            } catch (Exception ignored) { LOG.debug("Unable to check database.connector string. "); }

        }

        // Validate CoOccurrenceBasedSelector
        if (spotters.contains(SpotterPolicy.NESpotter)) {
            if (!new File(getOpenNLPModelDir()).exists())
                throw new ConfigurationException(String.format("OpenNLP model directory was not found. It is required by %s.", NESpotter.class));
            setOpenNLPModelsURI();
        }

        //Validate AhoCorasickSpotter
        if(spotters.contains(SpotterPolicy.AhoCorasickSpotter))
        {
            //Load spotter configuration:
            spotterSurfaceForms = config.getProperty("org.dbpedia.spotlight.spot.ahocorasick.surfaceforms").trim();
            if(!new File(spotterSurfaceForms).isFile()) {
                throw new ConfigurationException("Cannot find surfaceForms file "+spotterSurfaceForms);
            }
        }

    }


    private void setOpenNLPModelsURI()
    {
        openNLPModelsURI.put(OpenNLPUtil.OpenNlpModels.person.toString(), getOpenNLPPerson());
        openNLPModelsURI.put(OpenNLPUtil.OpenNlpModels.organization.toString(), getOpenNLPOrganization() );
        openNLPModelsURI.put(OpenNLPUtil.OpenNlpModels.location.toString(),getOpenNLPLocation() );
    }

    public Map<String, String> getOpenNLPModelsURI()
    {
        return openNLPModelsURI;
    }


    public String getCoOcSelectorDatabaseDriver() {
        return config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "database.jdbcdriver");
    }

    public String getCoOcSelectorDatabaseConnector() {
        return config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "database.connector");
    }

    public String getCoOcSelectorDatabaseUser() {
        return config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "database.user");
    }

    public String getCoOcSelectorDatabasePassword() {
        return config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "database.password");
    }

    public String getCoOcSelectorClassifierNGram() {
        return config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "classifier.ngram");
    }

    public String getCoOcSelectorClassifierUnigram() {
        return config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "classifier.unigram");
    }

    public String getCoOcSelectorDatasource() {
        return config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "datasource");
    }

    public String getSpotterFile() {
        return spotterFile;
    }

    public String getOpenNLPModelDir() {
        return config.getProperty(PREFIX_OPENNLP + "dir");
    }

    private String getOpenNLPPerson()
    {
        return config.getProperty(PREFIX_OPENNLP + "person", "http://dbpedia.org/ontology/Person");
    }

    private String getOpenNLPOrganization()
    {
        return config.getProperty(PREFIX_OPENNLP + "organization", "http://dbpedia.org/ontology/Organisation");
    }


    public String getOpenNLPLocation()
    {
        return config.getProperty(PREFIX_OPENNLP + "location", "http://dbpedia.org/ontology/Place");
    }


    // /data/spotlight/3.7/kea/keaModel-1-3-1
    public String getKeaModel() {
        return config.getProperty("org.dbpedia.spotlight.spot.kea.model");
    }

    public int getKeaMaxNumberOfPhrases() {
        return new Integer(config.getProperty("org.dbpedia.spotlight.spot.kea.maxNumberOfPhrases","1000"));
    }

    public int getKeaCutoff() {
        return new Integer(config.getProperty("org.dbpedia.spotlight.spot.kea.cutoff","-1"));
    }

    public List<SpotterPolicy> getSpotterPolicies() throws ConfigurationException {
        List<SpotterPolicy> policies = new ArrayList<SpotterPolicy>();
        String requestedSpotters = config.getProperty("org.dbpedia.spotlight.spot.spotters", "").trim();
        List<String> spotterNames = Arrays.asList(requestedSpotters.split(","));
        if (requestedSpotters.isEmpty() || spotterNames.size()==0) throw new ConfigurationException("Could not find 'org.dbpedia.spotlight.spot.spotters'. Please specify a comma-separated list of spotters to be loaded.");
        for (String s: spotterNames) {
            try {
                policies.add(SpotterPolicy.valueOf(s.trim()));
            } catch (java.lang.IllegalArgumentException e) {
                throw new ConfigurationException(String.format("Unknown spotter '%s' specified in 'org.dbpedia.spotlight.spot.spotters'.",s));
            }
        }
        return policies;
    }

    public String getSpotterSurfaceForms() {
        return spotterSurfaceForms;
    }


}
