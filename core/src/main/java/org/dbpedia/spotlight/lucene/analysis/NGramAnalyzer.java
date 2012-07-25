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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.position.PositionFilter;

/**
 * @author pablomendes
 */
public class NGramAnalyzer extends Analyzer {

    private int minGram;
    private int maxGram;

    public NGramAnalyzer(int minGram, int maxGram) {
        this.minGram = minGram;
        this.maxGram = maxGram;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        TokenStream s = new NGramTokenizer(reader, minGram, maxGram);
        s = new PositionFilter(s);
        return s;
    }

    public static void main(String[] args) throws IOException {
        String myString = "cancer";
        Analyzer analyzer = new NGramAnalyzer(3,3);
        System.out.println("Analyzing: \"" + myString +"\"");
        StringReader reader = new StringReader(myString);
        TokenStream stream = analyzer.tokenStream("field", reader);
//        TokenStream stream = new NGramTokenizer(reader, EdgeNGramTokenizer.Side.BACK, 1,2);
        stream.reset();

        // print all tokens until stream is exhausted
        while (stream.incrementToken()) {
            System.out.println("token: "+stream);
        }

        stream.end();
        stream.close();
    }
}