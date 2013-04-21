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

import org.dbpedia.spotlight.exceptions.IndexException;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.IOException;

/**
 * Indexes occurrences of (surface form, uri) in some text (context).
 * Each occurrences becomes a Lucene document
 * 
 * @author pablomendes
 */
public class SeparateOccurrencesIndexer extends OccurrenceContextIndexer {

    public int numEntriesIndexed = 0;
    
    public SeparateOccurrencesIndexer(LuceneManager lucene) throws IOException {
        super(lucene);
    }

    /**
     * This method just adds an occurrence to the index without trying to merge.
     * It is used by the indexer for definition pages ({@link SeparateOccurrencesIndexer}).
     * If you want to merge all occurrences of a given DBpediaResource into the same vector, see {@link MergedOccurrencesContextIndexer}).
     * @param r
     * @throws IndexException
     */
    public void add(DBpediaResourceOccurrence r) throws IndexException {
        //TODO FIXME quick hack to run overnight.
        addOccurrence(new DBpediaResourceOccurrence(r.resource(), new SurfaceForm(r.surfaceForm().name().toLowerCase()), r.context(), r.textOffset(), r.provenance()));
        //addOccurrence(r);
        numEntriesIndexed++;
    }
}
