package org.apache.lucene.analysis.sk;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.KeywordMarkerFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link Analyzer} for Slovak language.
 * <p>
 * Supports an external list of stopwords (words that will not be indexed at
 * all). A default set of stopwords is used unless an alternative list is
 * specified.
 * </p>
 *
 * <a name="version"/>
 * <p>
 * You must specify the required {@link Version} compatibility when creating
 * SlovakAnalyzer:
 * <ul>
 * <li>As of 3.1, words are stemmed with {@link SlovakStemFilter}
 * <li>As of 2.9, StopFilter preserves position increments
 * <li>As of 2.4, Tokens incorrectly identified as acronyms are corrected (see
 * <a href="https://issues.apache.org/jira/browse/LUCENE-1068">LUCENE-1068</a>)
 * </ul>
 */
public final class SlovakAnalyzer extends ReusableAnalyzerBase {
    
    /** File containing default Slovak stopwords. */
    public final static String DEFAULT_STOPWORD_FILE = "stopwords.txt";
    
    /**
     * Returns a set of default Slovak-stopwords
     *
     * @return a set of default Slovak-stopwords
     */
	public static final Set<?> getDefaultStopSet(){
        return DefaultSetHolder.DEFAULT_SET;
	}
	
	private static class DefaultSetHolder {
        private static final Set<?> DEFAULT_SET;
        
        static {
            try {
                DEFAULT_SET = WordlistLoader.getWordSet(IOUtils.getDecodingReader(SlovakAnalyzer.class,
                                                                                  DEFAULT_STOPWORD_FILE, IOUtils.CHARSET_UTF_8), "#", Version.LUCENE_CURRENT);
            } catch (IOException ex) {
                // default set should always be present as it is part of the
                // distribution (JAR)
                throw new RuntimeException("Unable to load default stopword set");
            }
        }
	}
    
    
    /**
     * Contains the stopwords used with the {@link StopFilter}.
     */
	// TODO once loadStopWords is gone those member should be removed too in favor of StopwordAnalyzerBase
	private Set<?> stoptable;
    private final Version matchVersion;
    private final Set<?> stemExclusionTable;
    
    /**
     * Builds an analyzer with the default stop words ({@link #getDefaultStopSet()}).
     *
     * @param matchVersion Lucene version to match See
     *          {@link <a href="#version">above</a>}
     */
	public SlovakAnalyzer(Version matchVersion) {
        this(matchVersion, DefaultSetHolder.DEFAULT_SET);
	}
	
    /**
     * Builds an analyzer with the given stop words.
     *
     * @param matchVersion Lucene version to match See
     *          {@link <a href="#version">above</a>}
     * @param stopwords a stopword set
     */
    public SlovakAnalyzer(Version matchVersion, Set<?> stopwords) {
        this(matchVersion, stopwords, CharArraySet.EMPTY_SET);
    }
    
    /**
     * Builds an analyzer with the given stop words and a set of work to be
     * excluded from the {@link SlovakStemFilter}.
     *
     * @param matchVersion Lucene version to match See
     *          {@link <a href="#version">above</a>}
     * @param stopwords a stopword set
     * @param stemExclusionTable a stemming exclusion set
     */
    public SlovakAnalyzer(Version matchVersion, Set<?> stopwords, Set<?> stemExclusionTable) {
        this.matchVersion = matchVersion;
        this.stoptable = CharArraySet.unmodifiableSet(CharArraySet.copy(matchVersion, stopwords));
        this.stemExclusionTable = CharArraySet.unmodifiableSet(CharArraySet.copy(matchVersion, stemExclusionTable));
    }
    
    
    /**
     * Builds an analyzer with the given stop words.
     *
     * @param matchVersion Lucene version to match See
     *          {@link <a href="#version">above</a>}
     * @param stopwords a stopword set
     * @deprecated use {@link #SlovakAnalyzer(Version, Set)} instead
     */
    @Deprecated
    public SlovakAnalyzer(Version matchVersion, String... stopwords) {
        this(matchVersion, StopFilter.makeStopSet( matchVersion, stopwords ));
	}
    
    /**
     * Builds an analyzer with the given stop words.
     *
     * @param matchVersion Lucene version to match See
     *          {@link <a href="#version">above</a>}
     * @param stopwords a stopword set
     * @deprecated use {@link #SlovakAnalyzer(Version, Set)} instead
     */
    @Deprecated
    public SlovakAnalyzer(Version matchVersion, HashSet<?> stopwords) {
        this(matchVersion, (Set<?>)stopwords);
	}
    
    /**
     * Builds an analyzer with the given stop words.
     *
     * @param matchVersion Lucene version to match See
     *          {@link <a href="#version">above</a>}
     * @param stopwords a file containing stopwords
     * @deprecated use {@link #SlovakAnalyzer(Version, Set)} instead
     */
    @Deprecated
    public SlovakAnalyzer(Version matchVersion, File stopwords ) throws IOException {
        this(matchVersion, (Set<?>)WordlistLoader.getWordSet(
                                                             IOUtils.getDecodingReader(stopwords, IOUtils.CHARSET_UTF_8), matchVersion));
	}
    
    /**
     * Loads stopwords hash from resource stream (file, database...).
     * @param   wordfile    File containing the wordlist
     * @param   encoding    Encoding used (win-1250, iso-8859-2, ...), null for default system encoding
     * @deprecated use {@link WordlistLoader#getWordSet(Reader, String, Version) }
     *             and {@link #SlovakAnalyzer(Version, Set)} instead
     */
    // TODO extend StopwordAnalyzerBase once this method is gone!
    @Deprecated
    public void loadStopWords( InputStream wordfile, String encoding ) {
        setPreviousTokenStream(null); // force a new stopfilter to be created
        if ( wordfile == null ) {
            stoptable = CharArraySet.EMPTY_SET;
            return;
        }
        try {
            // clear any previous table (if present)
            stoptable = CharArraySet.EMPTY_SET;
            stoptable = WordlistLoader.getWordSet(IOUtils.getDecodingReader(wordfile,
                                                                            encoding == null ? IOUtils.CHARSET_UTF_8 : Charset.forName(encoding)), matchVersion);
        } catch ( IOException e ) {
            // clear any previous table (if present)
            // TODO: throw IOException
            stoptable = Collections.emptySet();
        }
    }
    
    /**
     * Creates
     * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
     * used to tokenize all the text in the provided {@link Reader}.
     *
     * @return {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
     *         built from a {@link StandardTokenizer} filtered with
     *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
     *         , and {@link SlovakStemFilter} (only if version is >= LUCENE_31). If
     *         a version is >= LUCENE_31 and a stem exclusion set is provided via
     *         {@link #SlovakAnalyzer(Version, Set, Set)} a
     *         {@link KeywordMarkerFilter} is added before
     *         {@link SlovakStemFilter}.
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName,
                                                     Reader reader) {
        final Tokenizer source = new StandardTokenizer(matchVersion, reader);
        TokenStream result = new StandardFilter(matchVersion, source);
        result = new LowerCaseFilter(matchVersion, result);
        result = new StopFilter( matchVersion, result, stoptable);
        if (matchVersion.onOrAfter(Version.LUCENE_31)) {
            if(!this.stemExclusionTable.isEmpty())
                result = new KeywordMarkerFilter(result, stemExclusionTable);
            result = new SlovakStemFilter(result);
        }
        return new TokenStreamComponents(source, result);
    }
}