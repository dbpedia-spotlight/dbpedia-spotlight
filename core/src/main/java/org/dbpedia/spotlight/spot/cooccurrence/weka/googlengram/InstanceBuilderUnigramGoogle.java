package org.dbpedia.spotlight.spot.cooccurrence.weka.googlengram;

import org.dbpedia.spotlight.spot.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.spot.cooccurrence.weka.InstanceBuilderUnigram;

/**
 * InstanceBuilder for instances based on the Google n-gram corpus.
 *
 * @author Joachim Daiber
 */
public class InstanceBuilderUnigramGoogle extends InstanceBuilderUnigram {

	public InstanceBuilderUnigramGoogle(OccurrenceDataProvider dataProvider) {
		super(dataProvider);

		this.unigramCorpusMax 		= 40000;
		this.unigramWebMin 			= 25000000;
		this.bigramLeftWebMin 		= 200000;
		this.bigramRightWebMin 		= 200000;
		this.trigramLeftWebMin 		= 600000;
		this.trigramMiddleWebMin	= 200000;
		this.trigramRightWebMin 	= 500000;

	}
	
}
