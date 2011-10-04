/*
 * Copyright 2011 Pablo Mendes, Max Jakob
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
import opennlp.tools.postag.POSModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;
import opennlp.tools.util.model.BaseModel;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 * Spotter that uses Named Entity Recognition (NER) models from OpenNLP. Only spots People, Organisations and Locations.
 *
 * @author Rohana Rajapakse implemented the class
 * @author pablomendes  adjustments to logging, class rename, integrated with the rest of architecture
 */
public class NESpotter implements Spotter {
    private final Log LOG = LogFactory.getLog(this.getClass());
    protected static BaseModel sentenceModel = null;
    protected static Map<String, Object[]> entityTypes = new HashMap<String, Object[]>() {
        {
            put(OpenNlpModels.person.toString(), null);
            put(OpenNlpModels.location.toString(), null);
            put(OpenNlpModels.organization.toString(), null);
        }
    };

    public NESpotter(String onlpModelDir) throws ConfigurationException {

        try {
            if (NESpotter.sentenceModel == null) {
                NESpotter.sentenceModel  = loadModel(onlpModelDir, "english/en-sent.zip", OpenNlpModels.SentenceModel.toString());
            }
            if (NESpotter.entityTypes.get(OpenNlpModels.person.toString()) == null) {
                buildNameModel(onlpModelDir, OpenNlpModels.person.toString(),  new URI("http://dbpedia.org/ontology/Person"));
            }
            if (NESpotter.entityTypes.get(OpenNlpModels.location.toString()) == null) {
                buildNameModel(onlpModelDir, OpenNlpModels.location.toString(), new URI("http://dbpedia.org/ontology/Place"));
            }
            if (NESpotter.entityTypes.get(OpenNlpModels.organization.toString()) == null) {
                buildNameModel(onlpModelDir, OpenNlpModels.organization.toString(), new URI("http://dbpedia.org/ontology/Organisation"));
            }
        } catch (Exception e) {
            throw new ConfigurationException("Error initializing NESpotter", e);
        }

    }

    protected BaseModel buildNameModel(String directoryPath, String modelType, URI typeUri) throws IOException {
        String fname = OpenNlpModels.valueOf(modelType).filename();
        String modelRelativePath = String.format("english/%s.zip",	fname);
        BaseModel model = loadModel(directoryPath, modelRelativePath, modelType);
        entityTypes.put(modelType, new Object[] { typeUri, model });
        return model;
    }


    public BaseModel loadOpenNlpModel(String modelType, InputStream in) throws IOException {
        OpenNlpModels m = OpenNlpModels.valueOf(modelType);
        BaseModel mdl = loadgivenmodeltype( m, in);
        return mdl;
    }


    private BaseModel loadgivenmodeltype(OpenNlpModels m, InputStream in) throws InvalidFormatException, IOException {
        BaseModel mdl = null;
        switch(m) {
            case TokenizerModel: {
                mdl = new TokenizerModel(in);
                LOG.info("OpenNLP5 Tokenizer Model loaded: " + mdl);
                break;
            }
            case SentenceModel: {
                mdl = new SentenceModel(in);
                LOG.info("OpenNLP5 Sentence Model loaded: " + mdl);
                break;
            }
            case POSModel: {
                mdl = new POSModel(in);
                LOG.info("OpenNLP5 POS Model loaded: " + mdl);
                break;
            }
            case person:
            case organization:
            case location:
            {
                mdl = new TokenNameFinderModel(in);
                LOG.info("OpenNLP5 TokenNameFinderModel Model loaded: " + mdl);
                break;
            }
            default: System.out.println("Unknown Model Type!");

        }
        return mdl;
    }


    /**Loads OpenNLP 5 models.
     * @param directoryPath Path of the FS directory. Used when creating/opening an InputStream to a file
     *        model file in the folder (direct file reading)
     * @param modelRelativePath This is the to the model file starting from a resource folder (i.e. when reading
     *   from a jar, this is the path of the model file in the jar file followed by the model file name.
     *   e.g. in case if model files are in a folder named "opennlp" in the jar file, then we can set "opennlp"
     *   to directorypath and "english/en-sent.zip" to model relativepath (note the modelfile en-sent.zip) is
     *   assumed to to be in opennlp/english/en-sent.zip.
     * @param modelType
     * @return
     * @throws IOException
     */
    protected BaseModel loadModel(String directoryPath, String modelRelativePath, String modelType) throws IOException {

        OpenNlpModels m = OpenNlpModels.valueOf(modelType);
        ClassLoader loader = this.getClass().getClassLoader();
        InputStream in = null;
        if (directoryPath != null && directoryPath.length() > 0) {
            // load custom models from the provided FS directory
            File modelData = new File(new File(directoryPath),
                    modelRelativePath);
            in = new FileInputStream(modelData);
            LOG.debug(String.format("** OpenNLP is Loading OpenNLP 1.5 from a given directory path: ", modelData.getAbsolutePath()));
        } else {
            // load default OpenNLP models from jars
            String resourcePath = "opennlp/" + modelRelativePath;
            in = loader.getResourceAsStream(resourcePath);
            LOG.debug(String.format("** OpenNLP is Loading OpenNLP 1.5 models by Regular class loading: ", in.getClass().getCanonicalName()));
            //            }
            if (in == null) {
                throw new IOException(String.format("Could not find resource: %s",resourcePath));
            }
        }
        return loadOpenNlpModel(modelType, in);
    }

    @Override
    public List<SurfaceFormOccurrence> extract(Text intext) {
        String text = intext.text();
        List<SurfaceFormOccurrence> ret = null;
        try {
            for (Map.Entry<String, Object[]> type : entityTypes.entrySet()) {
                List<SurfaceFormOccurrence> res = null;
                String typeLabel = type.getKey();
                Object[] typeInfo = type.getValue();
                URI typeUri = (URI) typeInfo[0];
                BaseModel nameFinderModel = (BaseModel) typeInfo[1];
                res =extractNameOccurrences(nameFinderModel, text);
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
            e.printStackTrace();
        }

        return ret;
    }

    @Override
    public String name() {
        return "NESpotter";
    }

    protected List<SurfaceFormOccurrence> extractNameOccurrences(BaseModel nameFinderModel, String text) {

        SentenceDetectorME sentenceDetector = new SentenceDetectorME((SentenceModel)sentenceModel);
        String[] sentences = sentenceDetector.sentDetect(text);
        Span[] sentenceEndings = sentenceDetector.sentPosDetect(text);
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

                    int entStart = sentencePositions[i] + tokenspan[span.getStart()].getStart();
                    int entEnd = sentencePositions[i] +tokenspan[span.getEnd()-1].getEnd();

                    /*
                    System.out.println("\n\nRR-NE Found = " + buf.toString());
                    System.out.println("Start = " + entStart);
                    System.out.println("End = " + entEnd);
                    System.out.println("Sentence = " + sentence);
                    System.out.println("Text = " + text);
                    */

                    SurfaceForm surfaceForm = new SurfaceForm(buf.toString());
                    SurfaceFormOccurrence sfocc =  new SurfaceFormOccurrence(surfaceForm, new Text(text), entStart);
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

    public enum OpenNlpModels {
        SentenceModel("en-sent"),
        TokenizerModel("en-token"),
        POSModel("en-pos-maxent"),
        person("en-ner-person"),
        organization("en-ner-organization"),
        location("en-ner-location");

        private final String name; // filename
        OpenNlpModels(String fname) {
            this.name = fname;
        }
        public String filename()   { return name; }

    }


}
