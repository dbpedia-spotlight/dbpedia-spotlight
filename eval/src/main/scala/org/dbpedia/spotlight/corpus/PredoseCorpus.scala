package org.dbpedia.spotlight.corpus

import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.model._
import java.io.File
import io.Source
import collection.mutable.ListBuffer

/**
 * Annotated text source for reading the corpus from the PREDOSE project at Knoesis
 *
 * @author pablomendes
 */

class PredoseCorpus(val lines: Iterator[String]) extends AnnotatedTextSource {

    override def name = "PREDOSE"

    override def foreach[U](f: AnnotatedParagraph => U) {

        val OccurrenceLine = """^(\d+)\t(\d+)\t(.+?)\t(\S+)$""".r

        var currentTextId = ""
        var currentTextItem = new Text("")
        var currentOccurrences = new ListBuffer[DBpediaResourceOccurrence]()
        var lastParagraph : AnnotatedParagraph = null
        lines.foreach(line => {
            //            println(line);
            if (line.isEmpty) {
                val p = new AnnotatedParagraph(currentTextId, currentTextItem, currentOccurrences.toList)
                currentTextId = ""
                currentTextItem = new Text("")
                currentOccurrences = new ListBuffer[DBpediaResourceOccurrence]()
                lastParagraph = p;
                f(p)
            } else {
                line match {
                    case OccurrenceLine(start, end, label, uri) => {
                        currentOccurrences += new DBpediaResourceOccurrence(new DBpediaResource(uri),
                            new SurfaceForm(label),
                            currentTextItem,
                            start.toInt)
                    }
                    case _ => {
                        currentTextId = this.name+line.hashCode.toString
                        currentTextItem = new Text(line)
                    }
                }
            }
        })
        f(lastParagraph)
    }

}

object PredoseCorpus {

    implicit def stringToFile(s: String) = new File(s)

    def fromFile(file: File) = {
        val lines = Source.fromFile(file).getLines
        new PredoseCorpus(lines)
    }

    def main(args: Array[String]) {
        val file = new File(args(0))

        PredoseCorpus.fromFile(file)
            .foreach(println)
    }

}
