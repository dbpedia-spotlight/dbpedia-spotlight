package org.dbpedia.spotlight.candidate.cooccurrence.features.training.lingpipe;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ChunkFactory;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.ChunkingImpl;
import com.aliasi.corpus.Corpus;
import com.aliasi.corpus.ObjectHandler;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.util.Pair;
import org.dbpedia.spotlight.candidate.cooccurrence.classification.CandidateClass;
import org.dbpedia.spotlight.candidate.cooccurrence.features.training.OccurrenceDataset;
import org.dbpedia.spotlight.candidate.cooccurrence.features.training.OccurrenceInstance;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.tagging.lingpipe.LingPipeFactory;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnnotatedChunkCorpus extends Corpus<ObjectHandler<Chunking>> {

	List<Chunking> chunkings = new ArrayList<Chunking>();

	public AnnotatedChunkCorpus(OccurrenceDataset occurrenceDataset) throws ItemNotFoundException {

		Pair<String,Integer> currentSentence = null;
		ChunkingImpl chunking = null;

		for(OccurrenceInstance occurenceInstance : occurrenceDataset.getInstances()) {

			Pair<String, Integer> sentence = null;
			try {
				sentence = occurenceInstance.getText().taggedTokenProvider().getSentence(occurenceInstance);
			} catch (ItemNotFoundException e) {
				System.err.println("Could not find sentence.");
			}

			if (chunking != null && currentSentence != null && sentence.equals(currentSentence)) {
				Chunk chunk = chunk(occurenceInstance, currentSentence);

				if (chunk != null)
					chunking.add(chunk);
			} else {

				if (chunking != null)
					chunkings.add(chunking);

				currentSentence = sentence;
				chunking = new ChunkingImpl(sentence.a());

				Chunk chunk = chunk(occurenceInstance, currentSentence);

				if (chunk != null)
					chunking.add(chunk);
			}
		}

		if (chunkings.size() > 0)
			chunkings.add(chunking);

	}

	public void visitTrain(ObjectHandler<Chunking> handler) {
		//TODO: split into training and test set

		for (Chunking chunking : chunkings)
			handler.handle(chunking);
	}

	public void visitTest(ObjectHandler<Chunking> handler) {

		for (Chunking chunking : chunkings)
			handler.handle(chunking);
	}

	static Chunk chunk(OccurrenceInstance occurenceInstance, Pair<String, Integer> sentence) {
		int start = occurenceInstance.getOffset() - sentence.b();
		int end = start + occurenceInstance.getSurfaceForm().length();

		CandidateClass candidateClass = occurenceInstance.getCandidateClass();

		if(candidateClass == CandidateClass.term)
			return ChunkFactory.createChunk(start, end, candidateClass.name());

		return null;
	}

	public static void main(String[] args) throws IOException, JSONException, ItemNotFoundException {

		LingPipeFactory.setSentenceModel(new IndoEuropeanSentenceModel());
		LingPipeFactory.setTaggerModelFile(new File("/Users/jodaiber/Documents/workspace/ba/Bachelor Thesis/02 Implementation/Features/POS/pos-en-general-brown.HiddenMarkovModel"));

		OccurrenceDataset trainingData = new OccurrenceDataset(new File("/Users/jodaiber/Documents/workspace/ba/Bachelor Thesis/01 Evaluation/02 Annotation/Software/custom/src/annotation/test.json"), OccurrenceDataset.Format.JSON);
		new AnnotatedChunkCorpus(trainingData);
	}

}