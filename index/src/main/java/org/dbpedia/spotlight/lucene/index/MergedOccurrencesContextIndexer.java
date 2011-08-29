/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.lucene.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.dbpedia.spotlight.exceptions.IndexException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.lucene.search.SeparateOccurrencesContextSearcher;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The intention of this class is to use an in-memory map to merge a lot of occurrences in memory
 * and only merge them with the disk every once in a while.
 * That is because lucene only offers updates through a delete+rewrite.
 * Therefore we have to read the entire doc into memory, merge with the occurrences we've found
 * and then write it back to disk.
 * 
 * @author pablomendes
 */
public class MergedOccurrencesContextIndexer extends OccurrenceContextIndexer {

    Log LOG = LogFactory.getLog(this.getClass());

    /**
     * Should be set to a large number according to your memory availability.
     *
     */
    int minNumDocsBeforeFlush;
    int maxMergesBeforeOptimize;
    boolean lastOptimize;
    int numberOfSegmentsForOptimize = 4;

    private int numMerges = 0;

    // Just a counter for testing later
    public int numEntriesProcessed = 0;
    double initialFreeMem = 0.0;

    /**
     * Will hold a buffer of documents to be sent to disk when minNumDocsBeforeFlush is reached or when close is called.
     * It is a map from URI (String) to Resource definition (lucene Document).
     * A resource definition is a mergeDoc1IntoDoc2 of all occurrences of that resource.
     */
    Map<String, Document> uriToDocumentMap = new HashMap<String,Document>(); // the buffer

    /**
     * Calls {@link org.dbpedia.spotlight.lucene.index.BaseIndexer} constructor
     * Uses default buffer size = X
     * @param lucene
     * @throws IOException
     */
    public MergedOccurrencesContextIndexer(LuceneManager.BufferedMerging lucene) throws IOException {
        super(lucene);
        this.minNumDocsBeforeFlush = lucene.minNumDocsBeforeFlush();
        this.maxMergesBeforeOptimize = lucene.maxMergesBeforeOptimize();
        this.lastOptimize = lucene.lastOptimize();
        this.initialFreeMem =  Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
        this.mWriter.setSimilarity(lucene.contextSimilarity());
        LOG.info("Initial free memory = "+this.initialFreeMem);
        LOG.info("Setting buffer size (minNumDocsBeforeFlush) = "+this.minNumDocsBeforeFlush);
        LOG.info("Number of merges before optimize = "+this.maxMergesBeforeOptimize);
    }

    /**
     * This method buffers minNumDocsBeforeFlush documents and merges them in memory before flushing to disk.
     *
     * @param occ a dbpedia resource occurrence
     * @throws IndexException
     */
    @Override
    public void add(DBpediaResourceOccurrence occ) throws IndexException {
        numEntriesProcessed++;
        
        String uri = occ.resource().uri();

        double gb = 1073741824;
        double totalMemory = Runtime.getRuntime().totalMemory();
        double maxMemory = Runtime.getRuntime().maxMemory() - mLucene.RAMBufferSizeMB();

        //double freeMemory = Runtime.getRuntime().freeMemory();
        double freeMemory = maxMemory - totalMemory;
        double usedMemory = 1024 + (initialFreeMem - freeMemory);
        double memPerDoc = usedMemory / (1+uriToDocumentMap.size());
        double maxDocsBeforeError = maxMemory / memPerDoc;

        if (numEntriesProcessed % 50000 == 0) {
            LOG.debug("Free memory: "+(freeMemory/gb)+"GB/"+(maxMemory/gb)+"GB (Buffer contains "+uriToDocumentMap.size()+" entries). MemPerDoc: "+memPerDoc);
            LOG.debug("Total memory: " + (totalMemory/gb) + "GB");
            LOG.debug("Processed "+ numEntriesProcessed +" occurrences. Allocated mem can hold an est. max of "+maxDocsBeforeError+" entries.");
            LOG.debug("Buffer uriToDocumentMap contains "+uriToDocumentMap.size()+ " entries.");
        }
        
        //Whenever we are close to fill up the memory, merge the buffer with disk and clear the buffer.
//        if ((uriToDocumentMap.size() >= minNumDocsBeforeFlush) &&  // merge based on raw count
//           (freeMemory < 0.5 * maxMemory)) { // merge based on memory usage

//        if ((Runtime.getRuntime().freeMemory() < 0.4 * gb) ||
//            (numEntriesProcessed > (0.7 * maxDocsBeforeError)) ) {

//        if (usedMemory > (maxMemory * 0.5)) {


        if ( (uriToDocumentMap.size() >= minNumDocsBeforeFlush) ||
             ((freeMemory < 1*gb) && (uriToDocumentMap.size() >= minNumDocsBeforeFlush*0.2))
            ) {
            
            LOG.info("Processed "+ numEntriesProcessed +" occurrences. Allocated mem can hold an est. max of "+maxDocsBeforeError+" entries.");
            LOG.info("Buffer uriToDocumentMap contains "+uriToDocumentMap.size()+ " entries.");

            merge(); // if writing to disk fails, an exception will be thrown and the buffer won't be emptied below

            // Clear the buffer, unless the disk operation above failed
            uriToDocumentMap = new HashMap<String,Document>();
            try {
                Runtime.getRuntime().gc();
            } catch (Exception e) {
                LOG.error("Error forcing garbage collection.");
            }
            
            try {
                LOG.info("Now committing...");
                mWriter.commit();
                LOG.info("Commit done.");
            } catch (Exception e) {
                throw new IndexException("Error running commit.",e);
            }

            /*
            Since Lucene 2.9 you don’t need to optimize in most cases, as search is working per-segment.
            Optimizing an index should only be done during maintenance or if you want to ship a static index e.g. on a DVD.
            For live installations, the merge policy will manage index segments for you fine and index reopen times and field cache for sorting will be working fast.
            http://mail-archives.apache.org/mod_mbox/lucene-java-user/201008.mbox/browser
            */
            //try {
                //if (numMerges % maxMergesBeforeOptimize == 0) {
                    //LOG.info("Optimizing index...");
                    //mWriter.optimize();
                    //LOG.info("Optimize done.");
                //}
            //} catch (Exception e) {
            //    throw new IndexException("Error running optimization.",e);
            //}

        } else {
            if (uriToDocumentMap.containsKey(uri)) {
                //LOG.trace("Adding occurrence to document;");
                mLucene.addOccurrenceToDocument(occ, uriToDocumentMap.get(uri));
            } else {
                //LOG.trace("Adding document;");
                uriToDocumentMap.put(uri, mLucene.addOccurrenceToDocument(occ, new Document()));
            }
            if (uriToDocumentMap.size() == minNumDocsBeforeFlush /2)
                LOG.debug("Buffer uriToDocumentMap contains "+uriToDocumentMap.size()+ " entries.");
        }
    }

    public void merge() throws IndexException {
        try {
            long bufferSize = uriToDocumentMap.size();
            SeparateOccurrencesContextSearcher searcher = new SeparateOccurrencesContextSearcher(this.mLucene);
            LOG.info("Merging "+bufferSize+" resources in memory with " +searcher.getNumberOfEntries()+" resources in disk.");
            int numUpdatedDocs = 0;
            for(String uri: uriToDocumentMap.keySet()) {                
                // Get document from buffer
                Document docForResource = uriToDocumentMap.get(uri);
                // Merge with documents from disk if there are any
                List<Document> occurrences = searcher.getOccurrences(new DBpediaResource(uri));
                for (Document occurrenceDoc: occurrences) {
                    docForResource = mLucene.merge(occurrenceDoc, docForResource); // adds occurrence to resource
                }
                // If no merges were needed, add document
                if (occurrences.size() == 0) {
                    mWriter.addDocument(docForResource);
                } else { // Otherwise, update existing document.
                    numUpdatedDocs++;
                    Term uriTerm = new Term(LuceneManager.DBpediaResourceField.URI.toString(), uri);
                    mWriter.updateDocument(uriTerm, docForResource); //deletes everything with this uri and writes a new doc
                }
            }

            double percent = 0.0;
            if (numUpdatedDocs * bufferSize > 0) // Account for when either of these numbers is zero. 
                percent = new Double(numUpdatedDocs)/bufferSize;

            LOG.info("Merge done ("+percent+" resources merged).");
            numMerges++;

            searcher.close();
        } catch (IOException e) {
            throw new IndexException(e);
        } catch (SearchException e) {
            throw new IndexException(e);
        }
    }

    @Override
    public void close() throws IOException {
        //flush the remaining documents because if the last chunk didn't reach minNumDocsBeforeFlush they won't be indexed
        try {
            merge();
            if (lastOptimize) {
                LOG.info("Last optimization of index before closing...");
                mWriter.optimize(numberOfSegmentsForOptimize);
                LOG.info("Done.");
            }

            LOG.info("Expunging deletes from index before closing...");
            mWriter.expungeDeletes();
            LOG.info("Done.");
            mWriter.commit();
            mWriter.close();
            LOG.info("Index closed.");
        } catch (IndexException e) {
            throw new IOException(e);
        }
    }
}
