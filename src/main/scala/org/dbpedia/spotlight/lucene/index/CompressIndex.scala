package org.dbpedia.spotlight.lucene.index

import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.store.FSDirectory
import java.io.File
import scala.collection.JavaConversions._


/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 30.08.2010
 * Time: 11:13:10
 * To change this template use File | Settings | File Templates.
 */

object CompressIndex
{
    val unstoreFields = List( LuceneManager.DBpediaResourceField.CONTEXT, LuceneManager.DBpediaResourceField.SURFACE_FORM )

    val optimizeSegments = 4

    def main(args : Array[String]) {
        val indexFileName = args(0)
        
        val indexFile = new File(indexFileName)
        if (!indexFile.exists) {
            throw new IllegalArgumentException("index dir "+indexFile+" does not exists; can't compress")
        }
        val luceneManager = new LuceneManager.BufferedMerging(FSDirectory.open(indexFile))

        val compressor = new IndexEnricher(luceneManager)
        compressor.unstore(unstoreFields, optimizeSegments)
        compressor.close
    }

}