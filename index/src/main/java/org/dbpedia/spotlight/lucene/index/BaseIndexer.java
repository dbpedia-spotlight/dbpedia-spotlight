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

package org.dbpedia.spotlight.lucene.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.util.Version;
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

        //TODO this config is new in 3.6, does basically what LuceneManager does for us.
        IndexWriterConfig iwConfig = new IndexWriterConfig(Version.LUCENE_36,lucene.defaultAnalyzer());
        iwConfig.setOpenMode(create ? IndexWriterConfig.OpenMode.CREATE : IndexWriterConfig.OpenMode.APPEND);

        /* Determines ... buffering added documents and deletions before they are flushed to the Directory.
        NOTE: because IndexWriter uses ints when managing its internal storage,
        (...) it's best to set this value comfortably under 2048.
        http://lucene.apache.org/java/3_0_2/api/all/org/apache/lucene/index/IndexWriter.html#setRAMBufferSizeMB%28double%29
         */
        iwConfig.setRAMBufferSizeMB(lucene.RAMBufferSizeMB());

        /*
          Generally for faster indexing performance it's best to flush by RAM usage instead of document count and use as large a RAM buffer as you can.
          http://lucene.apache.org/java/3_0_2/api/all/org/apache/lucene/index/IndexWriter.html#setRAMBufferSizeMB%28double%29

          But if setting by doc count, the sweet spot suggested is 48 http://issues.apache.org/jira/browse/LUCENE-843
         */
        //this.mWriter.setMaxBufferedDocs(lucene.RAMBufferSizeMB());

        this.mWriter = new IndexWriter(lucene.directory(), iwConfig);

    }

    /**
     * Base class with the indexing functionality used by the subclasses {@link SeparateOccurrencesIndexer} and {@link MergedOccurrencesContextIndexer}}.
     * By default ({@link org.dbpedia.spotlight.lucene.LuceneManager#shouldOverwrite}=true) it creates the index or overwrites the existing one.
     * @param lucene
     * @throws IOException
     */
     public BaseIndexer(LuceneManager lucene) throws IOException {
        this(lucene, lucene.shouldOverwrite);
    }

    public abstract void add(T item) throws IndexException;


    @Override
    public void close() throws IOException {
        LOG.info("Closing Lucence writer...");
        mWriter.close();
        LOG.info("Done.");
    }
}
