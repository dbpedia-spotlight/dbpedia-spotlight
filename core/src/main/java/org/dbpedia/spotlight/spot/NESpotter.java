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

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;
import opennlp.tools.util.model.BaseModel;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.SpottingException;
import org.dbpedia.spotlight.model.Feature;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spotter that uses Named Entity Recognition (NER) models from OpenNLP. Only spots People, Organisations and Locations.
 *
 * TODO remove hardcoding of opennlp models. get from configuration
 *
 * @author Rohana Rajapakse (GOSS Interactive Limited) - implemented the class
 * @author pablomendes  adjustments to logging, class rename, integrated with the rest of architecture
 */
public class NESpotter implements Spotter {
    private final Log LOG = LogFactory.getLog(this.getClass());
    protected static BaseModel sentenceModel = null;
    protected static Map<String, Object[]> entityTypes = new HashMap<String, Object[]>() {
        {
            put(OpenNLPUtil.OpenNlpModels.person.toString(), null);
            put(OpenNLPUtil.OpenNlpModels.location.toString(), null);
            put(OpenNLPUtil.OpenNlpModels.organization.toString(), null);
        }
    };

    public NESpotter(String onlpModelDir, String i18nLanguageCode, Map<String, String> openNLPModelsURI) throws ConfigurationException {

        try {
            if (NESpotter.sentenceModel == null) {
                NESpotter.sentenceModel  = OpenNLPUtil.loadModel(onlpModelDir, i18nLanguageCode + OpenNLPUtil.OpenNlpModels.SentenceModel.filename(), OpenNLPUtil.OpenNlpModels.SentenceModel.toString());
            }
            if (NESpotter.entityTypes.get(OpenNLPUtil.OpenNlpModels.person.toString()) == null) {
                buildNameModel(onlpModelDir,OpenNLPUtil.OpenNlpModels.person.toString(),  new URI(openNLPModelsURI.get(OpenNLPUtil.OpenNlpModels.person.toString())),i18nLanguageCode);
            }
            if (NESpotter.entityTypes.get(OpenNLPUtil.OpenNlpModels.location.toString()) == null) {
                buildNameModel(onlpModelDir, OpenNLPUtil.OpenNlpModels.location.toString(), new URI(openNLPModelsURI.get(OpenNLPUtil.OpenNlpModels.location.toString())),i18nLanguageCode);
            }
            if (NESpotter.entityTypes.get(OpenNLPUtil.OpenNlpModels.organization.toString()) == null) {
                buildNameModel(onlpModelDir, OpenNLPUtil.OpenNlpModels.organization.toString(), new URI(openNLPModelsURI.get(OpenNLPUtil.OpenNlpModels.organization.toString())),i18nLanguageCode);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Error initializing NESpotter", e);
        }

    }

    protected BaseModel buildNameModel(String directoryPath, String modelType, URI typeUri, String i18nLanguageCode) throws IOException, ConfigurationException {
        String fname = OpenNLPUtil.OpenNlpModels.valueOf(modelType).filename();
        String modelRelativePath = i18nLanguageCode + fname;
        BaseModel model = OpenNLPUtil.loadModel(directoryPath, modelRelativePath, modelType);
        entityTypes.put(modelType, new Object[] { typeUri, model });
        return model;
    }


    @Override
    public List<SurfaceFormOccurrence> extract(Text text) throws SpottingException {

        List<SurfaceFormOccurrence> ret = new ArrayList<SurfaceFormOccurrence>();
        try {
            for (Map.Entry<String, Object[]> type : entityTypes.entrySet()) {
                List<SurfaceFormOccurrence> res = null;
                //TODO pass type information within SurfaceFormOccurrence to later stages
                String typeLabel = type.getKey();
                Object[] typeInfo = type.getValue();
                URI typeUri = (URI) typeInfo[0];
                BaseModel nameFinderModel = (BaseModel) typeInfo[1];
                res = extractNameOccurrences(nameFinderModel, text, typeUri);
                if (res != null && !res.isEmpty()) {
                    if (ret == null) {
                        ret = res;
                    }
                    else {
                        ret.addAll(res);
                    }
                }

            }
        } catch (Exception e) {
            throw new SpottingException(e);
        }

        return ret;
    }

    String name = "NESpotter";

	@Override
	public String getName() {
		return name;
	}
    @Override
    public void setName(String n) {
        this.name = n;
    }

    protected List<SurfaceFormOccurrence> extractNameOccurrences(BaseModel nameFinderModel, Text text, URI oType) {
        String intext = text.text();
        SentenceDetectorME sentenceDetector = new SentenceDetectorME((SentenceModel)sentenceModel);
        String[] sentences = sentenceDetector.sentDetect(intext);
        Span[] sentenceEndings = sentenceDetector.sentPosDetect(intext);
        int[] sentencePositions = new int[sentences.length + 1];
        for (int k=0; k<sentenceEndings.length; k++) {
            sentencePositions[k] = sentenceEndings[k].getStart();
        }

        NameFinderME finder = new NameFinderME((TokenNameFinderModel)nameFinderModel);

        List<SurfaceFormOccurrence> sfOccurrences = new ArrayList<SurfaceFormOccurrence>();
        Tokenizer tokenizer = new SimpleTokenizer();
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            //LOG.debug("Sentence: " + sentence);

            // extract the names in the current sentence
            String[] tokens = tokenizer.tokenize(sentence);
            Span[] tokenspan = tokenizer.tokenizePos(sentence);
            Span[] nameSpans = finder.find(tokens);
            double[] probs = finder.probs();

            if (nameSpans != null && nameSpans.length > 0) {
                //System.out.println("Tokens: " +(new ArrayList(Arrays.asList(tokens))).toString());
                //System.out.println("NameSpans: " +(new ArrayList(Arrays.asList(nameSpans))).toString());
                for (Span span : nameSpans) {
                    StringBuilder buf = new StringBuilder();
                    //System.out.println("StartSpan: " + span.getStart() + " EndSpan: " + span.getEnd());
                    for (int j = span.getStart(); j < span.getEnd(); j++) {
                        //System.out.println(tokens[i] + " appended to " + buf.toString());
                        buf.append(tokens[j]);
                        if(j<span.getEnd()-1) buf.append(" ");
                    }
                    String surfaceFormStr = buf.toString().trim();
                    if (surfaceFormStr.contains(".")) {
                    	surfaceFormStr = correctPhrase(surfaceFormStr, sentence);
                    }
                    
                    int entStart = sentencePositions[i] + tokenspan[span.getStart()].getStart();
                    int entEnd = sentencePositions[i] +tokenspan[span.getEnd()-1].getEnd();

                    /*
                    System.out.println("\n\nRR-NE Found = " + buf.toString());
                    System.out.println("Start = " + entStart);
                    System.out.println("End = " + entEnd);
                    System.out.println("Sentence = " + sentence);
                    System.out.println("Text = " + text);
                    */

                    SurfaceForm surfaceForm = new SurfaceForm(surfaceFormStr);
                    SurfaceFormOccurrence sfocc =  new SurfaceFormOccurrence(surfaceForm, text, entStart);
                    sfocc.features().put("type", new Feature("type",oType.toString()));
                    sfOccurrences.add(sfocc);
                }
            }

        }
        finder.clearAdaptiveData();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Occurrences found: "   +StringUtils.join(sfOccurrences, ", "));
        }
        return sfOccurrences;
    }
	private String correctPhrase(String phrs, String intext) {
		//first remove " ."
		while (phrs.contains(" .")){
			phrs = phrs.replace(" .", ".");
		}
		if (!intext.contains(phrs)) {
			while (phrs.contains(". ")){
			phrs = phrs.replace(". ", ".");
			}
		}
		//System.out.println(phrs);
		return phrs;
	}
   

}
