package org.dbpedia.spotlight.candidate.cooccurrence.weka.ukwac;

import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.candidate.cooccurrence.weka.InstanceBuilderNGram;

/**
 * Instance builder for instances based on co-occurrence data from the
 * UKWAC corpus.
 *
 * @author Joachim Daiber
 */
public class InstanceBuilderNGramUKWAC extends InstanceBuilderNGram {
	
	public InstanceBuilderNGramUKWAC(OccurrenceDataProvider dataProvider) {
		super(dataProvider);
		
		this.bigramLeftWebMin 	= -100;
		this.bigramRightWebMin 	= -100;
		this.trigramLeftWebMin 	= 200;
		this.trigramRightWebMin = 50;

	}
	
}
