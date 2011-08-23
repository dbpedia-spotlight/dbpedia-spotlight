package org.dbpedia.spotlight.model;

import com.sun.corba.se.impl.oa.poa.Policies;
import org.dbpedia.spotlight.exceptions.ConfigurationException;

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

    Properties config = new Properties();

    public enum SpotterPolicy {UserProvidedSpots,
                               LingPipeSpotter,
                               AtLeastOneNounSelector,
                               CoOccurrenceBasedSelector
                               }

	protected String spotterFile    = "";

	protected String candidateDatabaseDriver = "";
	protected String candidateDatabaseConnector = "";
	protected String candidateDatabaseUser = "";
	protected String candidateDatabasePassword = "";

	protected String candidateClassifierUnigram = "";
	protected String candidateClassifierNGram = "";
	protected String candidateOccurrenceDataSource = "";

	public String getCandidateDatabaseDriver() {
		return candidateDatabaseDriver;
	}

	public String getCandidateDatabaseConnector() {
		return candidateDatabaseConnector;
	}

	public String getCandidateDatabaseUser() {
		return candidateDatabaseUser;
	}

	public String getCandidateDatabasePassword() {
		return candidateDatabasePassword;
	}

	public String getCandidateClassifierNGram() {
		return candidateClassifierNGram;
	}

	public String getCandidateClassifierUnigram() {
		return candidateClassifierUnigram;
	}
	
	public String getSpotterFile() {
		return spotterFile;
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


		//Load spot selector configuration:
		candidateDatabaseDriver =
				config.getProperty("org.dbpedia.spotlight.candidate.cooccurence.database.jdbcdriver", "").trim();
		candidateDatabaseConnector =
				config.getProperty("org.dbpedia.spotlight.candidate.cooccurence.database.connector", "").trim();
		candidateDatabaseUser =
				config.getProperty("org.dbpedia.spotlight.candidate.cooccurence.database.user", "").trim();
		candidateDatabasePassword =
				config.getProperty("org.dbpedia.spotlight.candidate.cooccurence.database.password", "").trim();

		candidateClassifierUnigram =
				config.getProperty("org.dbpedia.spotlight.candidate.cooccurence.classifier.unigram", "").trim();
		candidateClassifierNGram =
						config.getProperty("org.dbpedia.spotlight.candidate.cooccurence.classifier.ngram", "").trim();

		candidateOccurrenceDataSource =
				config.getProperty("org.dbpedia.spotlight.candidate.cooccurence.datasource", "").trim();


	}

	public String getCandidateOccurrenceDataSource() {
		return candidateOccurrenceDataSource;
	}


}
