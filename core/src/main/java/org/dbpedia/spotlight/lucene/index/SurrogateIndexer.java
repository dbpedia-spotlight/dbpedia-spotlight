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
import org.apache.lucene.store.FSDirectory;
import org.dbpedia.spotlight.exceptions.IndexException;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.Surrogate;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * Class to index surrogates mapping (surface form -> set of resources) in lucene.
 */
public class SurrogateIndexer extends BaseIndexer<Surrogate> {

    final static Log LOG = LogFactory.getLog(BaseIndexer.class);

    /**
     * Constructs a surrogate indexer that follows the policy specified by the {@link LuceneManager} implementation used.
     * @param indexManager For a caseInsensitive behavior, use {@link org.dbpedia.annotation.lucene.LuceneManager.CaseInsensitiveSurfaceForms}.
     * @throws java.io.IOException
     */
    public SurrogateIndexer(LuceneManager indexManager) throws IOException {
        super(indexManager);
    }

    public void add(SurfaceForm surfaceForm, DBpediaResource resource) throws IndexException {
        Document newDoc = mLucene.createDocument(surfaceForm, resource);
        try {
            mWriter.addDocument(newDoc); // do not commit for faster indexing.
        } catch (IOException e) {
            throw new IndexException("Error adding occurrence to the index. ", e);
        }

        LOG.debug("Added to " + mLucene.directory().toString() + ": " + surfaceForm.toString() + " -> " + resource.toString());
    }

    public void add(Surrogate surrogate) throws IndexException {
        add(surrogate.surfaceForm(), surrogate.resource());
    }

    /**
     * Index surrogates mapping from a triples file.
     */
    public void addFromNTfile(File surfaceFormsDataSet) throws IOException, IndexException {
        LOG.info("Indexing surrogates from "+surfaceFormsDataSet.getName()+" to "+mLucene.directory()+"...");

        NxParser nxParser = new NxParser(new FileInputStream(surfaceFormsDataSet), false);
        while (nxParser.hasNext()) {
            Node[] nodes = nxParser.next();
            String resourceString = nodes[0].toString().replace("http://dbpedia.org/resource/","");
            String surfaceFormString = nodes[2].toString();
            add(new SurfaceForm(surfaceFormString), new DBpediaResource(resourceString));
        }

        LOG.info("Done.");
    }

    /**
     * Index surrogates mapping from a tab separted file.
     */
    public void addFromTSVfile(File surfaceFormsDataSet) throws IOException, IndexException {
        LOG.info("Indexing surrogates from "+surfaceFormsDataSet.getName()+" to "+mLucene.directory()+"...");

        String separator = "\t";
        Scanner tsvScanner = new Scanner(new FileInputStream(surfaceFormsDataSet), "UTF-8");

        while(tsvScanner.hasNextLine()) {
            String[] line = tsvScanner.nextLine().split(separator);
            String surfaceFormString = line[0];
            String resourceString = line[1];
            add(new SurfaceForm(surfaceFormString), new DBpediaResource(resourceString));
        }

        LOG.info("Done.");
    }

    /**
     * Optimize the index to speed up queries.
     *
     * @throws java.io.IOException
     */
    public void optimize() throws IOException {
        LOG.info("Optimizing surrogate index in "+mLucene.directory()+" ...");
        mWriter.optimize();
        LOG.info("Done.");
    }


    public static void main(String[] args) throws IOException, IndexException {
        String inputFileName = args[0];  // DBpedia surface forms mapping
		String outputDirName = args[1];  // target Lucene mContextIndexDir

        LuceneManager mLucene = new LuceneManager.CaseInsensitiveSurfaceForms(FSDirectory.open(new File(outputDirName)));

        SurrogateIndexer si = new SurrogateIndexer(mLucene);
        if (inputFileName.toLowerCase().endsWith(".nt")) {
            si.addFromNTfile(new File(inputFileName));
        }
        else if (inputFileName.toLowerCase().endsWith(".tsv")) {
            si.addFromTSVfile(new File(inputFileName));
        }
        si.optimize();
        si.close();
    }
}
