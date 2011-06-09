package org.dbpedia.spotlight.candidate.cooccurrence.weka.ukwac;

import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.candidate.cooccurrence.weka.InstanceBuilderNGram;

/**
 * @author Joachim Daiber
 */
public class InstanceBuilderNGramUKWAC extends InstanceBuilderNGram {
	
	public InstanceBuilderNGramUKWAC(OccurrenceDataProvider dataProvider) {
		super(dataProvider);
		
		this.bigramLeftWebMin 	= 40;
		this.bigramRightWebMin 	= 130;
		this.trigramLeftWebMin 	= 100;
		this.trigramRightWebMin = 50;

	}
	
}
