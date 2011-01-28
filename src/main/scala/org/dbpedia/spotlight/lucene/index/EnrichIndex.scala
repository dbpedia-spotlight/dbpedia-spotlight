package org.dbpedia.spotlight.lucene.index

import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.store.FSDirectory
import java.io.File
import org.dbpedia.spotlight.util.{SurrogatesUtil, TypesLoader}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 30.08.2010
 * Time: 11:13:10
 * To change this template use File | Settings | File Templates.
 */

object EnrichIndex
{

    def main(args : Array[String]) {
        val indexFileName = args(0)
        val typesMappingTSVFileName = args(1)
        val surrogatesFileName = args(2)

        val lowerCased = true

        val indexFile = new File(indexFileName)
        if (!indexFile.exists)
            throw new IllegalArgumentException("index dir "+indexFile+" does not exists; can't add surface forms")
        val luceneManager = new LuceneManager.BufferedMerging(FSDirectory.open(indexFile))

        val indexer = new IndexEnricher(luceneManager)
        val reverseSurrogatesMap = SurrogatesUtil.getReverseSurrogatesMap_java(new File(surrogatesFileName), lowerCased)
        val typesMap = TypesLoader.getTypesMap_java(new File(typesMappingTSVFileName));

        indexer.enrich(reverseSurrogatesMap, typesMap, false)

        indexer.close
    }

}