package org.dbpedia.spotlight.lucene.index

import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.store.FSDirectory
import java.io.File
import org.dbpedia.spotlight.io.{TypeAdder, FileOccurrenceSource}
import org.dbpedia.spotlight.util.TypesLoader
import org.dbpedia.spotlight.model.{DBpediaType, DBpediaResource}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 30.08.2010
 * Time: 11:13:10
 * To change this template use File | Settings | File Templates.
 */

object AddTypesToIndex
{

    def main(args : Array[String]) {
        val indexFileName = args(0)
        val typesMappingTSVFileName = args(1)

        val indexFile = new File(indexFileName)
        if (!indexFile.exists)
            throw new IllegalArgumentException("index dir "+indexFile+" does not exists; can't add types")
        val luceneManager = new LuceneManager.BufferedMerging(FSDirectory.open(indexFile))  //TODO buffered mergind right here?

        val typesIndexer = new IndexEnricher(luceneManager)
        val typesMap = TypesLoader.getTypesMap_java(new File(typesMappingTSVFileName));
        typesIndexer.enrich(null, typesMap)
        typesIndexer.close
    }

}
