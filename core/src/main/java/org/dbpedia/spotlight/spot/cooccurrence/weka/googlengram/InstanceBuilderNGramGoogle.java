package org.dbpedia.spotlight.spot.cooccurrence.weka.googlengram;

import org.dbpedia.spotlight.spot.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.spot.cooccurrence.weka.InstanceBuilderNGram;

/**
 * InstanceBuilder for instances based on the Google n-gram corpus.
 *
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
