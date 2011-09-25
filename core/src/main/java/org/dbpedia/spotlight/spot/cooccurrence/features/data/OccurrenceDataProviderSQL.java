package org.dbpedia.spotlight.spot.cooccurrence.features.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SpotterConfiguration;

import java.sql.*;
import java.util.List;

/**
 * Provides Co-occurrence data for unigrams, bigrams and trigrams using any SQL-based database
 * via a JDBC driver specified in the configuration file.
 *
 * @author Joachim Daiber
 * @author pablomendes (changed initialization to accept configuration without hardcoded filename)
 */

public class OccurrenceDataProviderSQL implements OccurrenceDataProvider {

	Connection sqlConnection;
	private final Log LOG = LogFactory.getLog(this.getClass());

	private static OccurrenceDataProviderSQL INSTANCE;

	public OccurrenceDataProviderSQL(SpotterConfiguration spotterConfiguration) throws InitializationException {

		try {
			Class.forName(spotterConfiguration.getCoOcSelectorDatabaseDriver()).newInstance();

			this.sqlConnection = DriverManager.getConnection(spotterConfiguration.getCoOcSelectorDatabaseConnector(),
					spotterConfiguration.getCoOcSelectorDatabaseUser(),
					spotterConfiguration.getCoOcSelectorDatabasePassword()
			);

		} catch (SQLException e) {
			throw new InitializationException("SQL Exception: Maybe wrong filename, incorrect JDBC connector String?", e);
		} catch (ClassNotFoundException e) {
			throw new InitializationException("Did not find SQL driver class specified in configuration file, " +
					"is the driver in the classpath?", e);
		} catch (InstantiationException e) {
			throw new InitializationException("Could not instantiate SQL driver.", e);
		} catch (IllegalAccessException e) {
			throw new InitializationException("No access to SQL driver specified in configuration file.", e);
		}

	}

	public OccurrenceDataProviderSQL(Connection sqlConnection) {
		this.sqlConnection = sqlConnection;
	}

	public static OccurrenceDataProviderSQL getInstance() {
		return INSTANCE;
	}

	/**
	 * Initialize the occurrence data provider. In the singleton pattern, this would be equivalent to the
	 * first call to getInstance(), however, since the initialization can fail for reasons of missing or
	 * wrong configuration, this should be done in this method.
	 *
	 * @param spotterConfiguration SpotterConfiguration with JDBC configuration.
	 * @throws org.dbpedia.spotlight.exceptions.InitializationException there was in error in the configuration
	 */

	public static void initialize(SpotterConfiguration spotterConfiguration) throws InitializationException {

		INSTANCE = new OccurrenceDataProviderSQL(spotterConfiguration);

	}

	@Override
	public CandidateData getCandidateData(String candidate) throws ItemNotFoundException {

		try {
			PreparedStatement statement
					= this.sqlConnection.prepareStatement("SELECT * FROM words WHERE word=? LIMIT 1;");
			statement.setString(1, candidate);
			ResultSet resultSet = statement.executeQuery();

			if(!resultSet.next())
				throw new ItemNotFoundException("Could not find information about candidate \"" + candidate +  "\".");

			return new CandidateData(resultSet.getLong("id"), candidate, resultSet.getLong("count_corpus"), resultSet.getLong("count_web"));
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public CoOccurrenceData getBigramData(CandidateData word1, CandidateData word2) throws ItemNotFoundException {
		try {

			Statement statement = this.sqlConnection.createStatement();
			ResultSet resultSet = statement.executeQuery
					("SELECT * FROM bigrams WHERE " +
							"word1=" + word1.getId() + " AND word2=" + word2.getId() + " " +
							"LIMIT 1;");

			if(!resultSet.next())
				throw new ItemNotFoundException("Could not find bigram.");

			return new CoOccurrenceData(
					0, 0,
					0, resultSet.getLong("significance_web"));

		} catch (SQLException e) {
			//Could not retrieve bigram information
			e.printStackTrace();
		}

		return null;
	}


	@Override
	/** {@inheritDoc} */
	public CoOccurrenceData getTrigramData(CandidateData word1, CandidateData word2, CandidateData word3) throws ItemNotFoundException {
		try {


			Statement statement = this.sqlConnection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT * FROM trigrams WHERE " +
					"word1=" + word1.getId() + " AND word2=" + word2.getId() + " AND word3="
					+ word3.getId() + " LIMIT 1;");


			if(!resultSet.next())
				throw new ItemNotFoundException("Could not find trigram.");

			return new CoOccurrenceData(0, resultSet.getLong("count_web"), 0, 0);

		} catch (SQLException e) {
			//Could not retrieve trigram information
			e.printStackTrace();
		}

		throw new ItemNotFoundException("Could not find trigram.");
	}

	@Override
	public List<CoOccurrenceData> getSentenceData(CandidateData candidate, List<String> tokens) {
		return null; //TODO implement
	}


}
