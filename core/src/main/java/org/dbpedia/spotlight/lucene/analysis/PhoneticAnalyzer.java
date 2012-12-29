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

package org.dbpedia.spotlight.lucene.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.phonetic.DoubleMetaphoneFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;
import org.dbpedia.spotlight.model.SpotlightConfiguration;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Set;

/**
 * Used for indexing text that can contain spelling anomalies
 *
 * @author pablomendes
 */
public class PhoneticAnalyzer extends Analyzer {

    private Set<String> mStopWordSet;
    private Version mMatchVersion;
    private int mMaxCodeLength = 8;

    public PhoneticAnalyzer(Version aMatchVersion, Set<String> aStopWordSet) {
        this.mStopWordSet = aStopWordSet;
        this.mMatchVersion = aMatchVersion;
    }

    public PhoneticAnalyzer(Version aMatchVersion, Set<String> aStopWordSet, int aMaxCodeLength) {
        this.mStopWordSet = aStopWordSet;
        this.mMatchVersion = aMatchVersion;
        this.mMaxCodeLength = aMaxCodeLength;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        TokenStream result = new StandardTokenizer(mMatchVersion, reader);
        result = new StandardFilter(mMatchVersion, result);
        result = new LowerCaseFilter(mMatchVersion, result);            // lowercased only
        result = new StopFilter(mMatchVersion, result, mStopWordSet);   // remove stopwords
        result = new DoubleMetaphoneFilter(result,mMaxCodeLength,true); // store phonetic code
        result = new ShingleFilter(result, 2, 3);                       // create token n-grams
        return result;
    }

    public static void main(String[] args) throws IOException {
        String myString = "This is a text";
        Analyzer analyzer = new PhoneticAnalyzer(Version.LUCENE_36,SpotlightConfiguration.DEFAULT_STOPWORDS);
        System.out.println("Analyzing: \"" + myString +"\"");
        StringReader reader = new StringReader(myString);
        TokenStream stream = analyzer.tokenStream("field", reader);
        stream.reset();

        // print all tokens until stream is exhausted
        while (stream.incrementToken()) {
            System.out.println("token: "+stream);
        }

        stream.end();
        stream.close();
    }
}
