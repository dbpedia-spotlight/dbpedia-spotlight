package org.dbpedia.spotlight.candidate.cooccurrence.features.training.lingpipe;

import com.aliasi.crf.ChainCrfFeatureExtractor;
import com.aliasi.crf.ChainCrfFeatures;
import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagger;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.IndoEuropeanTokenCategorizer;
import com.aliasi.util.AbstractExternalizable;
import com.aliasi.util.FastCache;
import com.aliasi.util.ObjectToDoubleMap;
import org.dbpedia.spotlight.candidate.cooccurrence.CandidateUtil;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.CandidateData;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProviderSQL;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;

import java.io.*;
import java.util.List;
import java.util.Map;



public class ChunkerFeatureExtractor
		implements ChainCrfFeatureExtractor<String>,
		Serializable {

	static final long serialVersionUID = -744675659290324L;

	static final File POS_HMM_FILE
			= new File("/Users/jodaiber/dbpdata/pos-en-general-brown.HiddenMarkovModel");

	// unserializable, so rebuild in constructor from file
	private final Tagger<String> mPosTagger;

	private final OccurrenceDataProviderSQL dataProvider;

    static SpotlightConfiguration mConfiguration;

	public ChunkerFeatureExtractor(SpotlightConfiguration configuration)
			throws ClassNotFoundException, IOException, ConfigurationException {

        this.mConfiguration = configuration;

		@SuppressWarnings("unchecked")
		HiddenMarkovModel posHmm
				= (HiddenMarkovModel)
				AbstractExternalizable
						.readObject(POS_HMM_FILE);

		FastCache<String,double[]> emissionCache
				= new FastCache<String,double[]>(100000);
		mPosTagger = new HmmDecoder(posHmm,null,emissionCache);

		dataProvider = OccurrenceDataProviderSQL.getInstance(configuration);
	}

	public ChainCrfFeatures<String> extract(List<String> tokens,
											List<String> tags) {
		return new ChunkerFeatures(tokens, tags);
	}

	Object writeReplace() {
		return new Externalizer(this);
	}


	class ChunkerFeatures extends ChainCrfFeatures<String> {
		private final Tagging<String> mPosTagging;
		public ChunkerFeatures(List<String> tokens,
							   List<String> tags) {
			super(tokens,tags);
			mPosTagging = mPosTagger.tag(tokens);
		}
		public Map<String,? extends Number> nodeFeatures(int n) {
			ObjectToDoubleMap<String> feats
					= new ObjectToDoubleMap<String>();

			boolean bos = n == 0;
			boolean eos = (n + 1) >= numTokens();

			String tokenCat = tokenCat(n);
			String prevTokenCat = bos ? null : tokenCat(n-1);
			String nextTokenCat = eos ? null : tokenCat(n+1);

			String token = normedToken(n);
			String prevToken = bos ? null : normedToken(n-1);
			String nextToken = eos ? null : normedToken(n+1);

			String posTag = mPosTagging.tag(n);
			String prevPosTag = bos ? null : mPosTagging.tag(n-1);
			String nextPosTag = eos ? null : mPosTagging.tag(n+1);

			/**
			 * Sentence position
			 */

			if (bos)
				feats.set("BOS",1.0);
			if (eos)
				feats.set("EOS",1.0);
			if (!bos && !eos)
				feats.set("!BOS!EOS",1.0);

			/**
			 * Token and neighbour tokens
			 */

			//feats.set("TOK_" + token, 1.0);
			//if (!bos)
			//	feats.set("TOK_PREV_" + prevToken,1.0);
			//if (!eos)
			//	feats.set("TOK_NEXT_" + nextToken,1.0);


			/**
			 * Token category
			 */

			feats.set("TOK_CAT_" + tokenCat, 1.0);
			if (!bos)
				feats.set("TOK_CAT_PREV_" + prevTokenCat, 1.0);
			if (!eos)
				feats.set("TOK_CAT_NEXT_" + nextToken, 1.0);


			/**
			 * POS of prev and next word
			 */

			feats.set("POS_" + posTag,1.0);
			if (!bos)
				feats.set("POS_PREV_" + prevPosTag,1.0);
			if (!eos)
				feats.set("POS_NEXT_" + nextPosTag,1.0);



			CandidateData candidateData = null;
			try {
				candidateData = dataProvider.getCandidateData(token(n));
			} catch (ItemNotFoundException e) {

			}

			if (candidateData != null) {

				if(candidateData.getCountCorpus() != null && candidateData.getCountCorpus() < CandidateUtil.UNIGRAM_CORPUS_MAX)
					feats.set("TOK_COUNT_CORPUS", candidateData.getCountCorpus());
				else
					feats.set("TOK_COUNT_CORPUS", 1000000);

				if(candidateData.getCountWeb() != null && candidateData.getCountWeb() > CandidateUtil.UNIGRAM_WEB_MIN)
					feats.set("TOK_COUNT_WEB", candidateData.getCountWeb());

			}

			return feats;
		}
		

		public Map<String,? extends Number> edgeFeatures(int n, int k) {
			ObjectToDoubleMap<String> feats
					= new ObjectToDoubleMap<String>();
			feats.set("PREV_TAG_" + tag(k),
					1.0);
			feats.set("PREV_TAG_TOKEN_CAT_"  + tag(k)
					+ "_" + tokenCat(n-1),
					1.0);


			return feats;
		}

		// e.g. 12/3/08 to *DD*/*D*/*DD*
		public String normedToken(int n) {
			return token(n).replaceAll("\\d+","*$0*").replaceAll("\\d","D");
		}

		public String tokenCat(int n) {
			return IndoEuropeanTokenCategorizer.CATEGORIZER.categorize(token(n));
		}

	}



	static class Externalizer extends AbstractExternalizable {
		static final long serialVersionUID = 4321L;
		private final ChunkerFeatureExtractor mExtractor;
		public Externalizer() {
			this(null);
		}
		public Externalizer(ChunkerFeatureExtractor extractor) {
			mExtractor = extractor;
		}

		public Object read(ObjectInput in)
				throws IOException, ClassNotFoundException {

			try {
				return new ChunkerFeatureExtractor(mConfiguration);
			} catch (ConfigurationException e) {
				e.printStackTrace();
				return null;
			}

		}

		public void writeExternal(ObjectOutput out)
				throws IOException {
			/* no op */
		}
	}



}


