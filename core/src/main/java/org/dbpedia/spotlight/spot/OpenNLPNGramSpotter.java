/*
 * Copyright 2011 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.spot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;
import opennlp.tools.util.model.BaseModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.StopAnalyzer;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.spot.Spotter;

/**
 * Consider as spots only the expressions marked as:
 * - named entities by the NER tagger,
 * - the noun-phrase chunks extracted by a shallow parser,
 * - all sub-expressions of up to 5 tokens of the noun-phrase chunks.
 *
 * This increases the coverage of NESpotter, which tends to annotate very little.
 *
 * The main advantage of this approach against the dictionary-based approach that we use now is that
 * it will not require 8GB of RAM to store a dictionary at runtime.
 *
 * TODO requires us to deal with overlaps when generating the annotations
 * TODO do not break chunks within quotes.
 * TODO do not get n-grams from NER spots, only from NP chunk spots
 *
 * @author Rohana Rajapakse (GOSS Interactive Limited) - implemented the class
 * @author pablomendes - adapted to integrate with architecture, made stopwords configurable and case insensitive, adjusted logging
 */
public class OpenNLPNGramSpotter implements Spotter {

	private final Log LOG = LogFactory.getLog(this.getClass());

	protected static BaseModel sentenceModel = null;
	protected static BaseModel chunkModel = null;
	protected static BaseModel tokenModel = null;
	protected static BaseModel posModel = null;
	protected Set<String> stopWords = StopAnalyzer.ENGLISH_STOP_WORDS_SET;


	//String directoryPath = "C:/software/appservers/dbp-spotlight-trunk3/data/models/opennlp/";  	//now reading from configuration properties

	String directoryPath = null;

	//Need OpenNLP modles. At present they are loaded in the constructor, but they should better be loaded at the startup of
	//dbpediaSpotlight to avoid re-loading them each time a request arrives. A singleton to hold the models would do.

	public OpenNLPNGramSpotter(String opennlpmodeldir) throws ConfigurationException {
        //directoryPath =  null; //for reading from dependency Jar files
        String directoryPath = opennlpmodeldir;

        if (OpenNLPNGramSpotter.sentenceModel == null) {
            OpenNLPNGramSpotter.sentenceModel  = OpenNLPUtil.loadModel(directoryPath, "english/en-sent.zip", OpenNLPUtil.OpenNlpModels.SentenceModel.toString());
        }
        if (OpenNLPNGramSpotter.chunkModel == null) {
            OpenNLPNGramSpotter.chunkModel  = OpenNLPUtil.loadModel(directoryPath, "english/en-chunker.zip", OpenNLPUtil.OpenNlpModels.ChunkModel.toString());
        }
        if (OpenNLPNGramSpotter.posModel == null) {
            OpenNLPNGramSpotter.posModel  = OpenNLPUtil.loadModel(directoryPath, "english/en-pos-maxent.zip", OpenNLPUtil.OpenNlpModels.POSModel.toString());
        }
        if (OpenNLPNGramSpotter.tokenModel == null) {
            OpenNLPNGramSpotter.tokenModel  = OpenNLPUtil.loadModel(directoryPath, "english/en-token.zip", OpenNLPUtil.OpenNlpModels.TokenizerModel.toString());
        }

    }

	@Override
	public List<SurfaceFormOccurrence> extract(Text intext) {

		//System.out.println("\n\nRR- extract(...) method called! with text: " + intext + "\n\n");
		String text = intext.text();
		//extracting NounPhrase nGrams
		List<SurfaceFormOccurrence> npNgrams = extractNPNGrams(text);
		/*
		System.out.println("\n\nAll NGrams of sentence:");
		System.out.println(intext + "\n");
		for( SurfaceFormOccurrence ng: npNgrams) {
			System.out.println(ng.surfaceForm() + " [" + ng.textOffset() + "]");
		}
		 */
		if (npNgrams != null && !npNgrams.isEmpty()) {
			return npNgrams;
		}
		else {
            return (List) new ArrayList<String>();
        }
	}

    String name = "OpenNLPNGramSpotter";

	@Override
	public String getName() {
		return name;
	}
    @Override
    public void setName(String n) {
        this.name = n;
    }


	/**Extracts noun-phrase n-grams from the given piece of input text. 
	 * @param intext
	 * @return A list of SurfaceFormOccurrence objects.
	 */
	protected List<SurfaceFormOccurrence> extractNPNGrams(String intext) {
		//System.out.println("\n\nRR- nextractNPNGrams(...) method called! with text: " + intext + "\n\n");
		List<SurfaceFormOccurrence> npNgramSFLst = new ArrayList<SurfaceFormOccurrence>();
		SentenceDetectorME  sentenceDetector = new SentenceDetectorME((SentenceModel)sentenceModel);
		TokenizerME tokenizer = new TokenizerME((TokenizerModel)tokenModel);
		POSTaggerME posTagger = new POSTaggerME((POSModel)posModel);
		ChunkerME chunker = new ChunkerME((ChunkerModel)chunkModel);

		Span[] sentSpans = sentenceDetector.sentPosDetect(intext);
		for (Span sentSpan : sentSpans) {
			String sentence = sentSpan.getCoveredText(intext).toString();
			int start = sentSpan.getStart();
			Span[] tokSpans = tokenizer.tokenizePos(sentence);
			String[] tokens = new String[tokSpans.length];
			// System.out.println("\n\nTokens:");
			for (int i = 0; i < tokens.length; i++) {
				tokens[i] = tokSpans[i].getCoveredText(sentence).toString();
				// System.out.println(tokens[i]);
			}
			String[] tags = posTagger.tag(tokens);
			Span[] chunks = chunker.chunkAsSpans(tokens, tags);
			for (Span chunk : chunks) {
				if ("NP".equals(chunk.getType())) {
					//Note: getStart()/getEnd() methods of Chunk spans only give the start and end token indexes of the chunk.
					//The actual Start/End positions of the chunk in the sentence need to be extracted from POS sentenceSpans.
					//They are offsets from the begining of the sentence in question. Need to add the start postion of the sentence
					//to compute the actual start/end offsets from the begining of the input text.
					int begin = tokSpans[chunk.getStart()].getStart();
					int end =   tokSpans[chunk.getEnd() - 1].getEnd();
					List<Map<String,Integer>> ngrampos = extractNGramPos(chunk.getStart(),chunk.getEnd() + -1);
					extractNGrams(ngrampos, start, intext, tokSpans, npNgramSFLst);
				}
			}
		}
		return npNgramSFLst;
	}
	
	public void extractNGrams(List<Map<String,Integer>> ngrampos, int start, String text, Span[] tokSpans, List<SurfaceFormOccurrence> sfOccurrences) {
		for( Map<String,Integer> mapelem: ngrampos) {
			int starttokenidx = mapelem.get("start");
			int endtokenidx = mapelem.get("end");
			//restrict to max 3-word phrases
			int noftkens = endtokenidx - starttokenidx;
			boolean ignorephrase = false;
			int begin = start + tokSpans[starttokenidx].getStart();
			int end =   start + tokSpans[endtokenidx].getEnd();
			String txtform = text.substring(begin,end);

			//Ignore phrases that contain more than 3-terms. It is unlikely that such long phrases to hit any resources. Need to experiment
			//with the cut-off value though.
			if ( noftkens > 2) 	ignorephrase = true;
			//ignore phrases starting with a stopword
			int starttkn_begin = start + tokSpans[starttokenidx].getStart();
			int starttkn_end = start + tokSpans[starttokenidx].getEnd();
			String starttknTxt = text.substring(starttkn_begin,starttkn_end);
			if (isStopWord(starttknTxt)) ignorephrase = true;
			//ignore phrases ending with a stopword
			int endtkn_begin = start + tokSpans[endtokenidx].getStart();
			int endtkn_end = start + tokSpans[endtokenidx].getEnd();
			String endtknTxt = text.substring(endtkn_begin,endtkn_end);
			if (isStopWord(endtknTxt)) ignorephrase = true;

			if (!ignorephrase) {								
				NGram ng = new NGram(txtform, begin, end);
				SurfaceForm surfaceForm = new SurfaceForm(ng.getTextform());
				SurfaceFormOccurrence sfocc =  new SurfaceFormOccurrence(surfaceForm, new Text(text), ng.getStart());
				if (!sfOccurrences.contains(sfocc)) {
					sfOccurrences.add(sfocc);
				}
			}
		}
	}
	  
	/**Generates a list of start/end tokens (indexes) of all sub=phrases/n-grams, given start and end token indexes
	 * e.g. if start token index and end token index are 5 and 7 (means token 5,6 and 7 makes up a noun phrase)
	 *      then generate [5], [5,6], [5,6,7], [6], [6,7] and [7] as sub-phrases (n-grams) of the the original phrase.
	 * @param startpos
	 * @param endpos
	 * @return A list of Maps. A Map element has only two keys "start" and "end".
	 */
	public List<Map<String,Integer>>  extractNGramPos(int startpos, int endpos) {
		List<Map<String,Integer>> ngrampos1 = new ArrayList<Map<String,Integer>>();
		if (startpos <=endpos) {

			for (int i = startpos; i <=endpos; i++) {
				for (int j=i; j<=endpos;j++) {
					int start = i;
					int end = j;
					Map<String,Integer> posmap = new HashMap<String,Integer>();
					posmap.put("start", start);
					posmap.put("end", end);
					ngrampos1.add(posmap);
				}
			}
		}
		return ngrampos1;
	}	

	/**Uses the stopWords from Lucene (StopAnalyzer.ENGLISH_STOP_WORDS_SET) to find if a given piece of text is
	 * a stopword.
	 * @param word
	 * @return true if the input text is a stopword.
	 */
	private boolean isStopWord(String word) {
		boolean ret = false;
		ret = stopWords.contains(word.toLowerCase());
		return ret;
	}
	
}
