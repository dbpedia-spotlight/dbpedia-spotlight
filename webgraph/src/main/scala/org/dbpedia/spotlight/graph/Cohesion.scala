package org.dbpedia.spotlight.graph


import java.io._
import org.apache.commons.logging.LogFactory
import io.Source
import it.unimi.dsi.law.rank.{PageRank, PageRankPowerMethod}
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import it.unimi.dsi.fastutil.io.BinIO
object Cohesion {

    val LOG = LogFactory.getLog(this.getClass)

    def getOutputFile(gFile: File, candidate: Int) = {
        new File(gFile+".PageRank%d".format(candidate))
    }

    def main(args: Array[String]) {

        val ntFile = new File("/home/pablo/data/dbpedia/page_links_en.nt")
        val mapFile = new File("/data/dbpedia/en/page_links_en.nt.redirectsResolved.cut.sorted.uniq.compressed")
        val dictFile = new File("/home/pablo/data/dbpedia/page_links_en_uris.set")

        LOG.info("Loading node dictionary.")
        val allNodes = Source.fromFile(dictFile).getLines()
        val nNodes = allNodes.size
        val gFile = new File(ntFile.getAbsolutePath+".WebGraph");

        val rootSetFile = new File("/home/pablo/eval/csaw/gold/candidates.set.compressed")

        val alpha = 0.85

        val fullGraphFile = new File(gFile.getAbsolutePath+"-underlying.graph")
        // Make sure we have parsed and serialized the file already
        val g = if (! fullGraphFile.exists) {
            LOG.info("Parsing graph. File %s does not exist. ".format(fullGraphFile.getAbsolutePath));
            WebGraphSerializerUtil.serialize(WebGraphSerializerUtil.parseMap(mapFile, nNodes), gFile)
        } else {
            LOG.info("Loading graph. File %s exists.".format(fullGraphFile.getAbsolutePath));
            WebGraphSerializerUtil.deserialize(gFile);
        }

        LOG.info("Starting Personalized PageRanks")
        val candidates = Source.fromFile(rootSetFile).getLines
        var preferences = DoubleArrayList.wrap(Array.fill(nNodes){ 0.0 });
        var last = 0;
        candidates.map(id => id.toInt)
            .filterNot(candidate => getOutputFile(gFile, candidate).exists() )
            .foreach( candidate => {
                LOG.info("PageRank for candidate %d.".format(candidate))
                preferences.set(last, 0.0)
                preferences.set(candidate, 1.0)
                last = candidate;
                val outputFile = getOutputFile(gFile,candidate)
                val pr = new PageRankPowerMethod(g);
                pr.stronglyPreferential = true
                pr.preference = preferences
                // cycle until we reach maxIter interations or the norm is less than the given threshold (whichever comes first)
                pr.stepUntil(PageRank.or(new PageRank.NormDeltaStoppingCriterion(PageRank.DEFAULT_THRESHOLD),
                                         new PageRank.IterationNumberStoppingCriterion(20))) // PageRank.DEFAULT_MAX_ITER
                LOG.info("Writing results to %s".format(outputFile.getAbsolutePath))
                BinIO.storeDoubles(pr.rank, outputFile.getAbsolutePath + ".ranks")
    //            val prop: Properties = pr.buildProperties(gFile.getAbsolutePath, null, candidate.toString)
    //            val propName : String = outputFile.getAbsolutePath+".properties"
    //            prop.save(propName)
        });
    }
}

