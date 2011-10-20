package org.dbpedia.spotlight.model;

import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.spot.CoOccurrenceBasedSelector;
import org.dbpedia.spotlight.spot.NESpotter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Configuration for Spotter and Spot Selection.
 *
 * @author Joachim Daiber
 * @author pablomendes - added spotter policies
 */
public class SpotterConfiguration {

    public final String PREFIX_COOCCURRENCE_SELECTOR = "org.dbpedia.spotlight.spot.cooccurrence.";

    Properties config = new Properties();
    protected String spotterFile    = "";

    public enum SpotterPolicy {Default,
        UserProvidedSpots,
        LingPipeSpotter,
        AtLeastOneNounSelector,
        CoOccurrenceBasedSelector,
        NESpotter,
        WikiMarkupSpotter
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
            String[] paramters = {"database.jdbcdriver", "database.connector", "database.user", "database.password",
                    "classifier.unigram", "classifier.ngram", "datasource"};

            for(String parameter : paramters) {
                try{
                    //Trim whitepsace from the paramter
                    config.setProperty(PREFIX_COOCCURRENCE_SELECTOR + parameter, config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + parameter).trim());
                }catch(NullPointerException e){
                    //One of the configuration property required for this Spot selector was not there.
                    throw new ConfigurationException(String.format("Cannot find required configuration property '%s' for co-occurrence based spot selector %s.",PREFIX_COOCCURRENCE_SELECTOR + parameter, CoOccurrenceBasedSelector.class));
                }
            }


            //Check if all the required files are there:
            String[] paramterFiles = {"classifier.unigram", "classifier.ngram"};
            for(String fileParamter : paramterFiles) {
                String file = config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + fileParamter);
                if(!new File(file).isFile()) {
                    throw new ConfigurationException("Cannot find required file '"+file+"' for configuration paramter "+PREFIX_COOCCURRENCE_SELECTOR + fileParamter);
                }
            }

            if(!config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "datasource").equalsIgnoreCase("ukwac") &&
               !config.getProperty(PREFIX_COOCCURRENCE_SELECTOR + "datasource").equalsIgnoreCase("google")) {
                throw new ConfigurationException(PREFIX_COOCCURRENCE_SELECTOR + "datasource must be one of 'ukwac' or 'google'.");
            }

        }

        // Validate CoOccurrenceBasedSelector
        if (spotters.contains(SpotterPolicy.NESpotter)) {
            if (!new File(getOpenNLPModelDir()).exists())
                throw new ConfigurationException(String.format("OpenNLP model directory was not found. It is required by %s.", NESpotter.class));
        }

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
        return config.getProperty("org.dbpedia.spotlight.spot.opennlp.dir");
    }

    public List<SpotterPolicy> getSpotterPolicies() throws ConfigurationException {
        List<SpotterPolicy> policies = new ArrayList<SpotterPolicy>();
        List<String> spotterNames = Arrays.asList(config.getProperty("org.dbpedia.spotlight.spot.spotters", "").trim().split(","));
        if (spotterNames.size()==0) throw new ConfigurationException("Could not find 'org.dbpedia.spotlight.spot.spotters'. Please specify a comma-separated list of spotters to be loaded.");
        for (String s: spotterNames) {
            try {
                policies.add(SpotterPolicy.valueOf(s.trim()));
            } catch (java.lang.IllegalArgumentException e) {
                throw new ConfigurationException(String.format("Unknown spotter '%s' specified in 'org.dbpedia.spotlight.spot.spotters'.",s));
            }
        }
        return policies;
    }


}
