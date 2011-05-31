package org.dbpedia.spotlight.candidate.cooccurrence.features.data;

import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;

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

	private static OccurrenceDataProviderSQL INSTANCE;

	public static OccurrenceDataProviderSQL getInstance(SpotlightConfiguration spotlightConfiguration) throws ConfigurationException {
        if (INSTANCE == null)
            INSTANCE = new OccurrenceDataProviderSQL(spotlightConfiguration);
        return INSTANCE;
    }

	private OccurrenceDataProviderSQL(SpotlightConfiguration spotlightConfiguration) throws ConfigurationException {

		try {
			Class.forName(spotlightConfiguration.getCandidateDatabaseDriver()).newInstance();

			this.sqlConnection = DriverManager.getConnection(spotlightConfiguration.getCandidateDatabaseConnector(),
					spotlightConfiguration.getCandidateDatabaseUser(),
					spotlightConfiguration.getCandidateDatabasePassword()
					);

		} catch (Exception e) {
			throw new ConfigurationException("Cannot get DB connection.", e);
        }
//        } catch (SQLException e) {
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		} catch (InstantiationException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		}


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
					resultSet.getLong("count_corpus"), resultSet.getLong("count_web"),
					resultSet.getFloat("significance_corpus"), resultSet.getFloat("significance_web"));

		} catch (SQLException e) {
			//Could not retrieve bigram information
			e.printStackTrace();
		}

		return null;
	}


	@Override
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
