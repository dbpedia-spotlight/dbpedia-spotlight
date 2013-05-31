package org.dbpedia.spotlight.spot;

import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;

/*
 * Original code: chapter 3.8 in http://alias-i.com/lingpipe-book/lingpipe-book-0.5.pdf
 *
 */
public class JAnnotationTokenizerFactory implements TokenizerFactory, Serializable {

    private Analyzer mAnalyzer;
    private org.apache.lucene.analysis.Tokenizer mTokenizer;
    private String mFieldName;


    public JAnnotationTokenizerFactory()  {

    }

    public JAnnotationTokenizerFactory(org.apache.lucene.analysis.Tokenizer mTokenizer) {
        this.mTokenizer = mTokenizer;
        //Only for surface forms?!
        mFieldName = LuceneManager.DBpediaResourceField.SURFACE_FORM.toString();
    }

    public JAnnotationTokenizerFactory(Analyzer mAnalyzer) {
        this.mAnalyzer = mAnalyzer;
        //Only for surface forms?!
        mFieldName = LuceneManager.DBpediaResourceField.SURFACE_FORM.toString();
    }

    static class TokenStreamTokenizer extends Tokenizer {
        private final TokenStream mTokenStream;
        private final TermAttribute mTermAttribute;
        private final OffsetAttribute mOffsetAttribute;
        private int mLastTokenStartPosition = -1;
        private int mLastTokenEndPosition = -1;

        public TokenStreamTokenizer(TokenStream tokenStream) {
            mTokenStream = tokenStream;
            mTermAttribute = mTokenStream.addAttribute(TermAttribute.class);
            mOffsetAttribute = mTokenStream.addAttribute(OffsetAttribute.class);
        }

        @Override
        public String nextToken() {
            try {
                if (mTokenStream.incrementToken()) {
                    mLastTokenStartPosition = mOffsetAttribute.startOffset();
                    mLastTokenEndPosition = mOffsetAttribute.endOffset();

                    if (mTermAttribute.term().contains(" ")) {
                        System.out.println("tokenization error:" + mLastTokenStartPosition + " " + mLastTokenStartPosition + " " + mTermAttribute.term());
                    }

                    return mTermAttribute.term();
                } else {
                    return null;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
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
        return new TokenStreamTokenizer(new StandardTokenizer(Version.LUCENE_36, reader));
    }
}
