/*
 * Copyright 2012 DBpedia Spotlight Development Team
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

import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.BaseModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.ConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author pablomendes
 */
public class OpenNLPUtil {

    private static final Log LOG = LogFactory.getLog(OpenNLPUtil.class);


    public enum OpenNlpModels {
        SentenceModel("en-sent"),
        ChunkModel("en-chunker"),
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
        public File file()   { return new File(name); }

    }

    public static BaseModel loadOpenNlpModel(String modelType, InputStream in) throws IOException {
        OpenNLPUtil.OpenNlpModels m = OpenNLPUtil.OpenNlpModels.valueOf(modelType);
        BaseModel mdl = loadgivenmodeltype( m, in);
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
    protected static BaseModel loadModel(String directoryPath, String modelRelativePath, String modelType) throws ConfigurationException {
        ClassLoader loader = OpenNLPUtil.class.getClassLoader();
        InputStream in = null;
        try {
            if (directoryPath != null && directoryPath.length() > 0) {
                // load custom models from the provided FS directory
                File modelData = new File(new File(directoryPath),	modelRelativePath);
                in = new FileInputStream(modelData);
                LOG.debug("**OpenNLP is Loading OpenNLP 1.5 " + modelType + " from a given directory path: " + modelData.getAbsolutePath());
            } else {
                // load default OpenNLP models from jars
                String resourcePath = "opennlp/" + modelRelativePath;
                in = loader.getResourceAsStream(resourcePath);
                LOG.debug("**OpenNLP is Loading OpenNLP 1.5 " + modelType + " model by Regular class loading: " + in.getClass().getCanonicalName());
                if (in == null) {
                    throw new IOException("could not find resource: " + resourcePath);
                }
            }
            return loadOpenNlpModel(modelType, in);
        } catch (IOException e) {
            throw new ConfigurationException("Could not load OpenNLP Model file.");
        }
    }

    protected static BaseModel loadgivenmodeltype(OpenNlpModels m, InputStream in) throws InvalidFormatException, IOException {
        BaseModel mdl = null;
        switch(m) {
            case TokenizerModel: {
                mdl = new TokenizerModel(in);
                LOG.debug("OpenNLP5 Tokenizer Model loaded: " + mdl);
                break;
            }
            case POSModel: {
                mdl = new POSModel(in);
                LOG.debug("OpenNLP5 POS Model loaded: " + mdl);
                break;
            }
            case SentenceModel: {
                mdl = new SentenceModel(in);
                LOG.debug("OpenNLP5 Sentence Model loaded: " + mdl);
                break;
            }
            case ChunkModel: {
                mdl = new ChunkerModel(in);
                LOG.debug("OpenNLP5 Sentence Model loaded: " + mdl);
                break;
            }
            case person:
            case organization:
            case location:
            {
                mdl = new TokenNameFinderModel(in);
                LOG.debug("OpenNLP5 TokenNameFinderModel Model loaded: " + mdl);
                break;
            }
            default: LOG.debug("Unknown Model Type!");

        }
        return mdl;
    }

}
