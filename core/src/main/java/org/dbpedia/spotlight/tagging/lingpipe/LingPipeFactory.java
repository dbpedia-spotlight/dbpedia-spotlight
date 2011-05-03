package org.dbpedia.spotlight.tagging.lingpipe;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tag.Tagger;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.Streams;

import java.io.*;

/**
 * Factory for LingPipe part-of-speech tagging, sentence and tokenization models.
 *
 * @author jodaiber
 */

public class LingPipeFactory {

	private static TokenizerFactory tokenizerFactory
			= new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");

	private static File sentenceModelFile;
	private static SentenceModel sentenceModel;

	private static File taggerModelFile;
	private static HiddenMarkovModel hmm;
	private static HmmDecoder hmmDecoder;

	public static void setTaggerModelFile(File taggerModelFile) throws IOException {
		LingPipeFactory.taggerModelFile = taggerModelFile;

		FileInputStream fileIn = null;
		HmmDecoder decoder = null;
		try {
			fileIn = new FileInputStream(taggerModelFile);
			ObjectInputStream objIn = new ObjectInputStream(fileIn);
			hmm = (HiddenMarkovModel) objIn.readObject();
			Streams.closeInputStream(objIn);
		} catch (FileNotFoundException e) {
			throw new IOException("Could not find POS tagger model "+taggerModelFile,e);
		} catch (ClassNotFoundException e) {
			throw new IOException("Could not decode POS tagger model in "+taggerModelFile,e);
		}
		hmmDecoder = new HmmDecoder(hmm);
	}


	public static void setSentenceModel(SentenceModel sentenceModel) {
		LingPipeFactory.sentenceModel = sentenceModel;
	}

	public static void setSentenceModelFile(File sentenceModelFile) {
		//TODO
		LingPipeFactory.sentenceModelFile = sentenceModelFile;
	}

	public static Tagger createPOSTagger() {
		return hmmDecoder;
	}

	public static SentenceModel createSentenceModel() {
		return sentenceModel;
	}

	public static void setTokenizerFactory(TokenizerFactory tokenizerFactory) {
		LingPipeFactory.tokenizerFactory = tokenizerFactory;
	}

	public static TokenizerFactory getTokenizerFactory() {
		return tokenizerFactory;
	}
}
