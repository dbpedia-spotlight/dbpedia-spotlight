package org.dbpedia.spotlight.spot.lingpipe

import org.dbpedia.spotlight.spot.Spotter
import java.io.{PrintWriter, File}
import org.dbpedia.spotlight.model.SpotlightConfiguration
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.log.SpotlightLog

object DumpLingPipeSpotterDict {
    def main(args : Array[String]) {
        val configuration: SpotlightConfiguration = new SpotlightConfiguration(args(0))
        val spotter = new LingPipeSpotter(new File(configuration.getSpotterConfiguration.getSpotterFile), configuration.getAnalyzer)
        //val lingPipeFactory = new LingPipeFactory(new File(configuration.getTaggerFile), new IndoEuropeanSentenceModel)
        val outFile = configuration.getSpotterConfiguration().getSpotterFile + ".out"
        SpotlightLog.info(this.getClass, "Writing spotter dictionary to file %s", outFile)
        val out = new PrintWriter(outFile)
        spotter.dictionary.entryList().foreach( e => out.println(e.phrase()) )
        out.close
        SpotlightLog.info(this.getClass, "Done.")
    }

}
