package org.dbpedia.spotlight.topic

import io.Source
import org.dbpedia.spotlight.model._
import collection.JavaConversions._
import java.io.{FileInputStream, FileWriter, PrintWriter}
import org.apache.commons.logging.LogFactory
import java.util.Properties

/**
 * Calculates topic priors of resources from index for all concept uris, given a topical classifier. This class was only implemented
 * for test purposes to calculate topical priors for resources from their contexts.
 */
object CalculateResourceTopicVectors {

    val LOG = LogFactory.getLog(getClass)

    /**
     *
     * @param args spotlight config (with at least topical information, index dir information, analyzer information and
     *             cachesize), conceptURIs file, output file
     */
    def main(args: Array[String]) {
        val config = new SpotlightConfiguration(args(0))

        val properties = new Properties()
        properties.load(new FileInputStream(args(0)))

        val factory = new SpotlightFactory(config)
        val searcher = factory.contextSearcher

        val classifier = factory.topicalClassifier

        val writer = new PrintWriter(new FileWriter(args(2)))
        var ctr = 0

        Source.fromFile(args(1)).getLines().foreach(resName => {
            val resource = new DBpediaResource(resName)

            val text = new Text(searcher.getContextWords(resource).toList.take(100).foldLeft("")((text, entry) => (entry.getKey + " ") * entry.getValue))
            if (text.text.length > 0) {
                val predictions = classifier.getPredictions(text)

                writer.println(resName + predictions.foldLeft("")((acc, prediction) => acc + " " + prediction._1.getName + ":" + prediction._2))
            }

            ctr += 1

            if (ctr % 10000 == 0)
                LOG.info(ctr + " resources processed")
        })

        writer.close()
    }

}
