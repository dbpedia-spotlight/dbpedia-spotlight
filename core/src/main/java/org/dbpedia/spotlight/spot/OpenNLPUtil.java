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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author pablomendes
 */
public class OpenNLPUtil {

    private static final Log LOG = LogFactory.getLog(OpenNLPUtil.class);


    public enum OpenNlpModels {
        SentenceModel("-sent"),
        ChunkModel("-chunker"),
        TokenizerModel("-token"),
        POSModel("-pos-maxent"),
        person("-ner-person"),
        organization("-ner-organization"),
        location("-ner-location");

        private final String name; // filename
        OpenNlpModels(String fname) {
            this.name = fname;
        }
        public String filename()   { return name + ".bin"; }
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
    
	protected static int computeOffset(String orgText, int newoffset, List<Integer> remidxes) {
		int offset = -1;
		int notremoved = 0;
		int removed = 0;
		for (int i = 0; i<orgText.length() && notremoved <= newoffset; i++) {
			if (remidxes.contains(new Integer(i))) {
				removed++;
			} else {
				notremoved++;
			}
		}
		
		offset = newoffset + removed;
		return offset;
	}	
	

	
	protected static List<Integer> chars2remove(String orgText) {
		
        //See: http://en.wikipedia.org/wiki/Quotation_mark_glyphs
        char[] charArray = { '"','\u002C','\u00AB','\u00BB','\u2018','\u2019','\u201A','\u201B','\u201C','\u201D','\u201E','\u201F','\u2039','\u203A'};
		String regexp = "[";
		for (Character ch: charArray) {
			regexp = regexp + ch;
		}
		regexp = regexp + "]";
		
		//System.out.println("\nregexp: " + regexp);
		List<Integer> remCharPosLst = new ArrayList<Integer>();

		Pattern p = Pattern.compile(regexp);
	    Matcher m = p.matcher(orgText); 

	    while (!m.hitEnd()) {
	     boolean mth = m.find();
	     if (mth) {
	    	 //System.out.println("Charater to remove: " + orgText.charAt(m.start()));
	    	 remCharPosLst.add(m.start());
	     }
	    }
		return remCharPosLst;
	}
	
	
	protected static String cleanText(String orgTxt, List<Integer> remCharIdxes) {
		String cleanTxt="";
		int start = 0;
		for (int idx: remCharIdxes) {
			cleanTxt = cleanTxt + orgTxt.substring(start, idx);
			start = idx + 1;
		}
		cleanTxt = cleanTxt + orgTxt.substring(start);
		
		return cleanTxt;
	}


}
