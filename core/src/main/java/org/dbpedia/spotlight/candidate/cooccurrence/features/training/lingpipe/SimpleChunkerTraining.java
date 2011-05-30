package org.dbpedia.spotlight.candidate.cooccurrence.features.training.lingpipe;

import com.aliasi.chunk.BioTagChunkCodec;
import com.aliasi.chunk.ChunkerEvaluator;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.TagChunkCodec;
import com.aliasi.corpus.Corpus;
import com.aliasi.corpus.ObjectHandler;
import com.aliasi.crf.ChainCrfChunker;
import com.aliasi.crf.ChainCrfFeatureExtractor;
import com.aliasi.io.LogLevel;
import com.aliasi.io.Reporter;
import com.aliasi.io.Reporters;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.stats.AnnealingSchedule;
import com.aliasi.stats.RegressionPrior;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.AbstractExternalizable;
import org.dbpedia.spotlight.candidate.cooccurrence.features.training.OccurrenceDataset;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.Filter;
import org.dbpedia.spotlight.candidate.cooccurrence.filter.FilterTermsize;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeFactory;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SimpleChunkerTraining {

	public static void main(String[] args) throws IOException, ItemNotFoundException, ClassNotFoundException, ConfigurationException, JSONException {

        SpotlightConfiguration config = new SpotlightConfiguration("conf/server.properties");
		LingPipeFactory.setSentenceModel(new IndoEuropeanSentenceModel());
		LingPipeFactory.setTaggerModelFile(new File("/Users/jodaiber/Documents/workspace/ba/Bachelor Thesis/02 Implementation/Features/POS/pos-en-general-brown.HiddenMarkovModel"));

		OccurrenceDataset trainingData = new OccurrenceDataset(new File("/Users/jodaiber/Documents/workspace/ba/Bachelor Thesis/01 Evaluation/02 Annotation/Software/custom/src/annotation/test.json"), OccurrenceDataset.Format.JSON);

		List<Filter> filters = new LinkedList<Filter>();
		filters.add(new FilterTermsize(FilterTermsize.Termsize.unigram));
		trainingData.filter(filters);


		Corpus<ObjectHandler<Chunking>> corpus = new AnnotatedChunkCorpus(trainingData);

		TokenizerFactory tokenizerFactory
				= IndoEuropeanTokenizerFactory.INSTANCE;
		boolean enforceConsistency = true;
		TagChunkCodec tagChunkCodec
				= new BioTagChunkCodec(tokenizerFactory,
				enforceConsistency);

		ChainCrfFeatureExtractor<String> featureExtractor
				= new ChunkerFeatureExtractor(config);

		int minFeatureCount = 1;

		boolean cacheFeatures = true;

		boolean addIntercept = true;

		double priorVariance = 4.0;
		boolean uninformativeIntercept = true;
		RegressionPrior prior
				= RegressionPrior.gaussian(priorVariance,
				uninformativeIntercept);
		int priorBlockSize = 3;

		double initialLearningRate = 0.05;
		double learningRateDecay = 0.995;
		AnnealingSchedule annealingSchedule
				= AnnealingSchedule.exponential(initialLearningRate,
				learningRateDecay);

		double minImprovement = 0.00001;
		int minEpochs = 10;
		int maxEpochs = 1000;

		Reporter reporter
				= Reporters.stdOut().setLevel(LogLevel.DEBUG);

		System.out.println("\nEstimating");
		ChainCrfChunker crfChunker
				= ChainCrfChunker.estimate(corpus,
				tagChunkCodec,
				tokenizerFactory,
				featureExtractor,
				addIntercept,
				minFeatureCount,
				cacheFeatures,
				prior,
				priorBlockSize,
				annealingSchedule,
				minImprovement,
				minEpochs,
				maxEpochs,
				reporter);

		File modelFile = new File(args[0]);
		System.out.println("\nCompiling to file=" + modelFile);
		AbstractExternalizable.serializeTo(crfChunker, modelFile);

		ChainCrfChunker compiledCrfChunker
				= (ChainCrfChunker)
				AbstractExternalizable.serializeDeserialize(crfChunker);
		System.out.println("     compiled");

		System.out.println("\nEvaluating");
		ChunkerEvaluator evaluator
				= new ChunkerEvaluator(compiledCrfChunker);

		corpus.visitTest(evaluator);
		System.out.println("\nEvaluation");
		System.out.println(evaluator);

	}

}