package org.dbpedia.spotlight.lucene.index

import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.store.FSDirectory
import org.dbpedia.spotlight.io.{TypeAdder, FileOccurrenceSource}
import org.dbpedia.spotlight.model.{DBpediaType, DBpediaResource}
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.util.{ConfigProperties, TypesLoader}

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 30.08.2010
 * Time: 11:13:10
 * To change this template use File | Settings | File Templates.
 */

object AddTypesToIndex
{
    val instanceTypesFileName = ConfigProperties.get("InstanceTypesDataset")

    def main(args : Array[String]) {
        val indexFileName = args(0)

        val indexFile = new File(indexFileName)
        if (!indexFile.exists) {
            throw new IllegalArgumentException("index dir "+indexFile+" does not exists; can't add types")
        }
        val luceneManager = new LuceneManager.BufferedMerging(FSDirectory.open(indexFile))

        val typesIndexer = new IndexEnricher(luceneManager)

        val typesMap = TypesLoader.getTypesMap_java(new FileInputStream(instanceTypesFileName));
        typesIndexer.enrichWithTypes(typesMap)
        typesIndexer.close
    }

}
