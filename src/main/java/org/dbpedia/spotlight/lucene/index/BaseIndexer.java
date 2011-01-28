package org.dbpedia.spotlight.lucene.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.dbpedia.spotlight.exceptions.IndexException;
import org.dbpedia.spotlight.model.vsm.FeatureVectorBuilder;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.Closeable;
import java.io.IOException;

/**
 * Implementation of a FeatureVectorBuilder that uses a Lucene index to store features.
 * Should not be instantiated.
 * TODO PABLO remove this class, create an occurrence context indexer and a wikipagecontext indexer?
 *
 * @author pablomendes
 */
public abstract class BaseIndexer<T> implements FeatureVectorBuilder, Closeable {

    final static Log LOG = LogFactory.getLog(BaseIndexer.class);

    LuceneManager mLucene;
    IndexWriter mWriter;

    /**
     * Base class with the indexing functionality used by the subclasses {@link SeparateOccurrencesIndexer} and {@link MergedOccurrencesContextIndexer}}.
     * @param lucene
     * @param create - what to do if lucene.mContextIndexDir already exists (so we do not unvoluntary add to an existing index). true to create the index or overwrite the existing one; false to append to the existing index
     * @throws IOException
     */
    public BaseIndexer(LuceneManager lucene, boolean create) throws IOException {
        this.mLucene = lucene;
        //TODO PABLO we can make the LuceneManager construct this guy, and all classes use the manager to obtain it. This renders BaseIndexer moot
        this.mWriter = new IndexWriter(lucene.directory(),
                                       lucene.contextAnalyzer(),
                                       create,
                                       new IndexWriter.MaxFieldLength(25000)); //TODO is 25000 enough?

        /* Determines ... buffering added documents and deletions before they are flushed to the Directory.
        NOTE: because IndexWriter uses ints when managing its internal storage,
        (...) it's best to set this value comfortably under 2048.
        http://lucene.apache.org/java/3_0_2/api/all/org/apache/lucene/index/IndexWriter.html#setRAMBufferSizeMB%28double%29
         */
        this.mWriter.setRAMBufferSizeMB(lucene.RAMBufferSizeMB());
        /*
          Generally for faster indexing performance it's best to flush by RAM usage instead of document count and use as large a RAM buffer as you can.
          http://lucene.apache.org/java/3_0_2/api/all/org/apache/lucene/index/IndexWriter.html#setRAMBufferSizeMB%28double%29

          But if setting by doc count, the sweet spot suggested is 48 http://issues.apache.org/jira/browse/LUCENE-843  
         */
        //this.mWriter.setMaxBufferedDocs(lucene.RAMBufferSizeMB());
    }

    /**
     * Base class with the indexing functionality used by the subclasses {@link SeparateOccurrencesIndexer} and {@link MergedOccurrencesContextIndexer}}.
     * By default ({@link org.dbpedia.spotlight.lucene.LuceneManager#shouldOverride}=true) it creates the index or overwrites the existing one.
     * @param lucene
     * @throws IOException
     */
     public BaseIndexer(LuceneManager lucene) throws IOException {
        this(lucene, lucene.shouldOverride);
    }

    public abstract void add(T item) throws IndexException;

 
    @Override
    public void close() throws IOException {
        LOG.info("Closing Lucence writer...");
        mWriter.close();
        LOG.info("Done.");
    }
}
