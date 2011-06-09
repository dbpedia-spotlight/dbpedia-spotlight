package org.dbpedia.spotlight.candidate.cooccurrence.weka.googlengram;

import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.candidate.cooccurrence.weka.InstanceBuilderNGram;

/**
 * @author Joachim Daiber
 */
public class InstanceBuilderNGramGoogle extends InstanceBuilderNGram {

	public InstanceBuilderNGramGoogle(OccurrenceDataProvider dataProvider) {
		super(dataProvider);
		
		this.bigramLeftWebMin 		= 20000;
		this.bigramRightWebMin 		= 20000;
		this.trigramLeftWebMin 		= 600000;
		this.trigramMiddleWebMin	= 200000;
		this.trigramRightWebMin 	= 500000;

	}
	
}
