package org.dbpedia.spotlight.spot.lingpipe;

import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;

/* Original code: chapter 3.8 in http://alias-i.com/lingpipe-book/lingpipe-book-0.5.pdf
 *
 */
public class JAnnotationTokenizerFactory implements TokenizerFactory, Serializable {

    private final Analyzer mAnalyzer;
    private final String mFieldName;

    public JAnnotationTokenizerFactory() {
        mAnalyzer = new BrazilianAnalyzer(Version.LUCENE_36);
        mFieldName = "SURFACE_FORM";
    }


    static class TokenStreamTokenizer extends Tokenizer {
        private final TokenStream mTokenStream;
        private final TermAttribute mTermAttribute;
        private final OffsetAttribute mOffsetAttribute;
        private int mLastTokenStartPosition = -1;
        private int mLastTokenEndPosition = -1;

        public TokenStreamTokenizer(TokenStream tokenStream) {
            mTokenStream = tokenStream;
            mTermAttribute
                    = mTokenStream.addAttribute(TermAttribute.class);
            mOffsetAttribute
                    = mTokenStream.addAttribute(OffsetAttribute.class);
        }

        @Override
        public String nextToken() {
            try {
                if (mTokenStream.incrementToken()) {
                    mLastTokenStartPosition
                            = mOffsetAttribute.startOffset();
                    mLastTokenEndPosition
                            = mOffsetAttribute.endOffset();
                    return mTermAttribute.term();
                } else {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }

        }

        @Override
        public int lastTokenStartPosition() {
            return mLastTokenStartPosition;
        }

        @Override
        public int lastTokenEndPosition() {
            return mLastTokenEndPosition;
        }


    }

    public Tokenizer tokenizer(char[] cs, int start, int len) {
        Reader reader = new CharArrayReader(cs, start, len);
        TokenStream tokenStream
                = mAnalyzer.tokenStream(mFieldName, reader);
        return new TokenStreamTokenizer(tokenStream);

    }
}
