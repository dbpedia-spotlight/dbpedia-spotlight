package org.dbpedia.spotlight.topical.index

import java.io.{FileWriter, PrintWriter, File}
import scala._
import org.dbpedia.spotlight.model.{DBpediaCategory, Topic}
import org.dbpedia.spotlight.topical.util.TopicUtil
import org.dbpedia.spotlight.io.{FileOccurrenceSource, FileOccsCategoriesSource}
import org.dbpedia.spotlight.log.SpotlightLog
import scala.collection.mutable._
import scala.util.control.Breaks._


/**
 * This object takes the splitted occs directory extracted by SplitOccsSemiSupervised,
 * the output file, where the corpus will be written.
 *
 * @author dirk
 */
object GenerateOccTopicCorpus {

    /**
     *
     * @param args 1st: path to splitted occs,
     *             2nd: output corpus file,
     *             3rd: optional: number of examples to be written for each topic (if <= 0, maximum number will be written),
     */
    def main(args: Array[String]) {
       generateCorpusWithEqualCount(new File(args(0)), new File(args(1)),if(args.length > 2) args(2).toInt else -1)
    }

    /**
     * Simplest way of generating a topic corpus from splitted occs, which takes examples from splitted occs by random,
     * until nrOfExamples is reached
     * @param splittedOccsDir
     * @param output
     * @param nrOfExamples <=0 means write maximum number of examples to corpus
     */
    def generateCorpus(splittedOccsDir: File, output: File,nrOfExamples: Int = -1) {
        output.getParentFile.mkdirs()
        val outputWriter = new PrintWriter(new FileWriter(output))

        splittedOccsDir.listFiles().foreach(topicFile => {
            val topic = new Topic(topicFile.getName.substring(0, topicFile.getName.length - 4))

            SpotlightLog.info(this.getClass, "======================= Processing %s =======================", topicFile.getName)

            if (!topic.getName.equals(TopicUtil.CATCH_TOPIC)) {
                var counter = 0
                FileOccurrenceSource.fromFile(topicFile).takeWhile(occ => {
                    outputWriter.println(topic.getName + "\t" + occ.context.text)
                    counter += 1
                    if (counter % 10000 == 0)
                        SpotlightLog.info(this.getClass, "%d examples written", counter)
                    nrOfExamples <= 0 || counter < nrOfExamples
                })

                outputWriter.flush()
            }
        })

        outputWriter.close()
        SpotlightLog.info(this.getClass, "Done")
    }

    /**
     * Simplest way of generating a topic corpus from splitted occs, which takes examples from splitted occs,
     * until nrOfExamples is reached
     * @param splittedOccsDir
     * @param nrOfExamples <=1 means write maximum number of examples to corpus
     * @param output
     * @deprecated
     */
    def generateCorpusWithEqualCount(splittedOccsDir: File, output: File,nrOfExamples: Int = -1) {
        output.getParentFile.mkdirs()
        val outputWriter = new PrintWriter(new FileWriter(output))

        var corpusSize = 0
        var sizes = Map[Topic, Int]()

        SpotlightLog.info(this.getClass, "======================= Counting occurrences in each split =======================")
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

        SpotlightLog.info(this.getClass, "Writing corpus with size %d for each topic", corpusSize)

        splittedOccsDir.listFiles().foreach(topicFile => {
            val topic = new Topic(topicFile.getName.substring(0, topicFile.getName.length - 4))

            SpotlightLog.info(this.getClass, "======================= Processing %s =======================", topicFile.getName)

            if (!topic.getName.equals(TopicUtil.CATCH_TOPIC)) {
                SpotlightLog.info(this.getClass, "======================= Extract %d occs to corpus =======================", corpusSize)
                val threshold = corpusSize.toDouble / sizes(topic)
                var counter = 0

                breakable {
                    FileOccurrenceSource.fromFile(topicFile).foreach(occ => {
                        if (counter < corpusSize) {
                            if (math.random <= threshold) {
                                outputWriter.println(topic.getName + "\t" + occ.context.text)
                                counter += 1
                                if (counter % 10000 == 0)
                                    SpotlightLog.info(this.getClass, "%d examples written", counter)
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
