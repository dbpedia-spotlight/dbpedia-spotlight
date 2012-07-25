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
 * Factory for LingPipe part-of-speech tagging, sentence segmentation and tokenization models.
 *
 * <p>Note that the part-of-speech tagger is currently held as Singleton instance.
 * This is only possible if the underlying HM model is thread-safe, for more information
 * on this issue, cf. the LingPipe
 * <a href="http://alias-i.com/lingpipe/docs/api/com/aliasi/hmm/HmmDecoder.html">HmmDecoder class</a>.
 * </p>
 * 
 * @author Joachim Daiber
 */

public class LingPipeFactory {

	/**
	 * Default RegEx-based word tokenizer.
	 */
	private TokenizerFactory tokenizerFactory
			= new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");

	private File sentenceModelFile;
	private SentenceModel sentenceModel;

	
	/**
	 * The Hidden Markov model part-of-speech tagger.
	 */
	private HmmDecoder hmmDecoder;

	
	/**
	 * Create a new LingPipe factory with the tagger model file and sentence model provided
	 * as parameters. On instantiation of the object, the HMM will be read from the tagger
	 * model file and the part-of-speech tagger and sentence segmentizer are created.
	 *
	 * @param taggerModelFile File containing the serialized hidden Markov model for PoS tagging
	 * @param sentenceModel Model for sentence segmentation
	 * @throws IOException The file was not found or could not be read.
	 */

	public LingPipeFactory(File taggerModelFile, SentenceModel sentenceModel) throws IOException {
		this.setTaggerModelFile(taggerModelFile);
		this.setSentenceModel(sentenceModel);
	}

	/**
	 * Set the tagger model file to taggerModelFile, read the model and
	 * create the HmmDecoder.
	 *
	 * @param taggerModelFile serialized HM model file
	 * @throws IOException The file was not found or could not be read.
	 */
	private void setTaggerModelFile(File taggerModelFile) throws IOException {

		FileInputStream fileIn;
		HiddenMarkovModel hmm;

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


	private void setSentenceModel(SentenceModel sentenceModel) {
		this.sentenceModel = sentenceModel;
	}

	private void setSentenceModelFile(File sentenceModelFile) {
		this.sentenceModelFile = sentenceModelFile;
		//TODO: enable serialized sentence models
	}

	/**
	 * Get the Singleton part-of-speech tagger instance.
	 *
	 * @return part-of-speech tagger instance
	 */
	public Tagger getPoSTaggerInstance() {
		return hmmDecoder;
	}

	/**
	 * Get the Singleton sentence segmentizer instance.
	 *
	 * @return sentence segmentizer instance.
	 */
	public SentenceModel getSentenceModelInstance() {
		return sentenceModel;
	}

	public void setTokenizerFactory(TokenizerFactory tokenizerFactory) {
		tokenizerFactory = tokenizerFactory;
	}


	/**
	 * Get the tokenizer factory instance that can be used for word
	 * tokenization.
	 *
	 * @return word tokenizer instance.
	 */
	public TokenizerFactory getTokenizerFactoryInstance() {
		return tokenizerFactory;
	}

}
