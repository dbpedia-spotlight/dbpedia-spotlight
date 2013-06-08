package org.dbpedia.spotlight.topical.index

import java.io.{FileWriter, PrintWriter, File}
import scala._
import org.dbpedia.spotlight.topical.wikipedia.util.{WikipediaFlattenedHierarchyLoader, WikipediaHierarchyLoader}
import org.dbpedia.spotlight.util.IndexingConfiguration
import org.dbpedia.spotlight.model.{DBpediaCategory, Topic}
import org.dbpedia.spotlight.topical.util.TopicUtil
import org.dbpedia.spotlight.io.{FileOccurrenceSource, FileOccsCategoriesSource}
import org.apache.commons.logging.LogFactory
import scala.collection.mutable._
import scala.util.control.Breaks._


/**
 * This object takes the splitted occs directory extracted by SplitOccsByCategories$ or SplitOccsSemiSupervised$,
 * dbpedias sorted file article_categories (http://downloads.dbpedia.org/3.7/en/article_categories_en.nt.bz2),
 * wikipedias hierarchy (http://downloads.dbpedia.org/3.7/en/skos_categories_en.nt),
 * the output directory from FlattenWikipediaHierarchy, the number of examples each corpus should contain and finally
 * the output file, where the corpus will be written. Note that this corpus should be shuffled afterwards
 *
 * @author dirk
 */
object GenerateOccTopicCorpus {

    private val LOG = LogFactory.getLog(getClass)

    /**
     *
     * @param args 1st: path to splitted occs, 2nd: number of examples to be written for each topic (if <= 0, maximum number will be written),
     *             3rd: output corpus file, 4th: indexing.properties
     */
    def main(args: Array[String]) {

        if (args.length >= 4) {
            val config = new IndexingConfiguration(args(3))
            generateCorpusFromTopics(new File(args(0)), new File(config.get("org.dbpedia.spotlight.data.sortedArticlesCategories")),
                new File(config.get("org.dbpedia.spotlight.topic.flattenedHierarchy")),
                args(1).toInt, new File(args(2)))
        }
        else
            generateCorpus(new File(args(0)), args(1).toInt, new File(args(2)))
    }

    /**
     * Simplest way of generating a topic corpus from splitted occs, which takes examples from splitted occs by random,
     * until nrOfExamples is reached
     * @param splittedOccsDir
     * @param nrOfExamples -1 means write maximum number of examples to corpus
     * @param output
     */
    def generateCorpus(splittedOccsDir: File, nrOfExamples: Int, output: File) {
        output.getParentFile.mkdirs()
        val outputWriter = new PrintWriter(new FileWriter(output))

        var corpusSize = 0
        var sizes = Map[Topic, Int]()

        LOG.info("======================= Counting occurrences in each split =======================")
        corpusSize = Int.MaxValue
        splittedOccsDir.listFiles().foreach(topicFile => {
            val topic = new Topic(topicFile.getName.substring(0, topicFile.getName.length - 4))
            var lineNr = 0
            scala.io.Source.fromFile(topicFile).getLines().foreach(_ => lineNr += 1)
            sizes += (topic -> lineNr)
            corpusSize = math.min(lineNr, corpusSize)
        })

        if (nrOfExamples > 1 && nrOfExamples < corpusSize)
            corpusSize = nrOfExamples

        LOG.info("Writing corpus with size " + corpusSize + " for each topic")

        splittedOccsDir.listFiles().foreach(topicFile => {
            val topic = new Topic(topicFile.getName.substring(0, topicFile.getName.length - 4))

            LOG.info("======================= Processing " + topicFile.getName + " =======================")

            if (!topic.getName.equals(TopicUtil.CATCH_TOPIC)) {
                LOG.info("======================= Extract " + corpusSize + " occs to corpus =======================")
                val threshold = corpusSize.toDouble / sizes(topic)
                var counter = 0

                breakable {
                    FileOccurrenceSource.fromFile(topicFile).foreach(occ => {
                        if (counter < corpusSize) {
                            if (math.random <= threshold) {
                                outputWriter.println(topic.getName + "\t" + occ.context.text)
                                counter += 1
                                if (counter % 10000 == 0)
                                    LOG.info(counter + " examples written")
                            }
                        }
                        else
                            break()

                    })
                }

                outputWriter.flush()
            }
        })

        outputWriter.close()
    }

    private class CategoryTuple(var parent: DBpediaCategory,
                                var children: Set[DBpediaCategory],
                                var full: Boolean,
                                var nrPages: Double,
                                var members: Double)

}
