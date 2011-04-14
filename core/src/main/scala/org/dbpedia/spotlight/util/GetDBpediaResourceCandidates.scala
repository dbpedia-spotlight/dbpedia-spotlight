package org.dbpedia.spotlight.util

import java.io.PrintWriter
import io.Source
import org.dbpedia.spotlight.model.DBpediaResource._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{SurfaceForm, DBpediaResource, LuceneFactory, SpotlightConfiguration}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.candidate.CommonWordFilter

/**
 * Takes in a list of Common Words and queries the index to find classes that are confusable with those.
 * @author pablomendes
 */

object GetDBpediaResourceCandidates {

    private val LOG = LogFactory.getLog(this.getClass)

    val configuration = new SpotlightConfiguration("conf/eval.properties"); //TODO move this to command line parameter

    val factory = new LuceneFactory(configuration)
    val searcher = factory.searcher

    /**
     * This class obtains DBpediaResources that are candidates for a given surface form
     *
     */
    def main(args: Array[String]) {
        val indexingConfigFileName = args(0)
        val surfaceFormSetFile = args(1)
        val uriSetFile = args(2)

        val out = new PrintWriter(uriSetFile);

        val surfaceForms = new CommonWordFilter(surfaceFormSetFile, false).parse;
        var i = 0;
        surfaceForms.foreach( name => {
            i = i + 1
            LOG.info(String.format("Surface Form %s : %s", i.toString, name));
            val sf = new SurfaceForm(name)
            val uriList = searcher.getDBpediaResourceCandidates(sf).toList.map( r => r.uri).mkString("\n")
            if (uriList.size>0)
                out.print(uriList+"\n");
            if (i % 100 == 0) {
                out.flush
            }
        });

        out.close
    }

}