package org.dbpedia.spotlight.model;

import org.dbpedia.spotlight.exceptions.ConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration for Spotter and Spot Selection.
 *
 * @author Joachim Daiber
 */
public class SpotterConfiguration {

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

	public SpotterConfiguration(String fileName) throws ConfigurationException {

		//Read config properties:
		Properties config = new Properties();
		try {
			config.load(new FileInputStream(new File(fileName)));
		} catch (IOException e) {
			throw new ConfigurationException("Cannot find configuration file "+fileName,e);
		}

		//Load spotter configuration:
		spotterFile = config.getProperty("org.dbpedia.spotlight.spot.dictionary").trim();
		if(!new File(spotterFile).isFile()) {
			throw new ConfigurationException("Cannot find spotter file "+spotterFile);
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
