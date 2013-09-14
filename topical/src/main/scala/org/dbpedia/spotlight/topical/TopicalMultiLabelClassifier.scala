package org.dbpedia.spotlight.topical

import org.dbpedia.spotlight.model.{Text, Topic}
import java.io._
import scala.collection.mutable._
import scala.Array
import org.dbpedia.spotlight.log.SpotlightLog
import io.Source
import collection.mutable

/**
 * Object that can train or load a WekaMultiLabelClassifier.
 *
 * @author dirk
 */
object TopicalMultiLabelClassifier {

    /**
     *
     * @param args path to training corpus in arff (class labels have to be last attribute), path to output directory
     */
    def main(args: Array[String]) {
        trainModel(new File(args(0)), new File(args(1)))
    }

    def trainModel(corpus: File, modelOut: File, classifierType:String = "") {
        SpotlightLog.info(this.getClass, "Training multilabel classifier on corpus: %s, saving model to: %s", corpus.getAbsolutePath, modelOut.getAbsolutePath)
        val trainer = TopicalClassifierTrainer.byType(classifierType)
        modelOut.mkdirs()
        val topics = mutable.Map[Topic,(File,PrintWriter)]()

        Source.fromFile(corpus).getLines.foreach(line => {
            val Array(topicStr,text) = line.split("\t",2)
            val topic = new Topic(topicStr)
            val pw = topics.getOrElseUpdate(topic, {
                val f = new File(corpus.getAbsolutePath+"-"+topic.getName)
                (f,new PrintWriter(f))
            })._2

            //Add this example to the topical corpus
            pw.println(line)

            topics.foreach {
                case (topic,(_,pw2)) => {
                    if (!pw.equals(pw2))
                        pw2.println(MultiLabelClassifier.NEGATIVE_TOPIC_PREFIX+topic.getName+"\t"+text)
                }
            }
        })

        topics.values.foreach(_._2.close())

        topics.foreach {
            case (topic,(file,_)) => {
                val model = trainer.trainModel(file)

                model.serialize(new File(modelOut,topic.getName+".model"))
            }
        }
    }


}

/**
 * Topical classifier implementation that wraps TopicalClassifiers (one for each topic) to build a MultiLabelClassifier
 *
 * @throws IOException
 */
class TopicalMultiLabelClassifier(modelsDir: File, classifierType:String) extends MultiLabelClassifier {
    var models = Map[Topic, TopicalClassifier]()

    {
        if (modelsDir.exists())
            modelsDir.listFiles(new FilenameFilter {
                def accept(file: File, name: String): Boolean = name.endsWith(".model")
            }).foreach(modelFile => {
                val name = modelFile.getName.replace(".model", "")
                models += (new Topic(name) -> TopicalClassifierFactory.fromFile(modelFile,classifierType).get)
            })
        else
            modelsDir.mkdirs()
    }

    def getPredictions(text: Text): Array[(Topic, Double)] = {
        var predictions = List[(Topic, Double)]()
        models.foreach {
            case (topic, classifier) => {
                predictions = (classifier.getPredictions(text).find(_._1.equals(topic)).get) :: predictions
            }
        }
        predictions.toArray
    }

    def getTopics(): List[Topic] = models.keySet.toList.sortBy(_.getName)

    def update(text: Text, topic: Topic) {
        models(topic).update(text,topic)
    }

    def serialize(to:File) {
        models.foreach {
            case (topic, model) => {
                model.serialize(to)
            }
        }
    }

    def updateNegative(text: Text, topic: Topic) {
        if (models.contains(topic))
            models(topic).update(text,new Topic(MultiLabelClassifier.NEGATIVE_TOPIC_PREFIX+topic.getName))
        else
            SpotlightLog.error(this.getClass, "Tried to update on not existing topic!")
    }

}
