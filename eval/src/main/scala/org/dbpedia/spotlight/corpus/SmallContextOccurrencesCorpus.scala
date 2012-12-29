package org.dbpedia.spotlight.corpus

import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.model.{Text, AnnotatedParagraph}
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 8/8/12
 * Time: 6:26 PM
 * To change this template use File | Settings | File Templates.
 */

class SmallContextOccurrencesCorpus(corpus:AnnotatedTextSource)  extends AnnotatedTextSource {
    override def name = "Small context - " + corpus.name

    override def foreach[U](f : AnnotatedParagraph => U) {
        corpus.foreach( paragraph => {
            val text = paragraph.text.text
            var ctr = 0
            paragraph.occurrences.foreach( occ => {
                val (leftSplit,rightSplit) = text.splitAt(occ.textOffset)
                val smallContext = leftSplit.split(" ").takeRight(10).mkString(" ")+" " + rightSplit.split(" ",12).take(11).mkString(" ")

                val annotated = new AnnotatedParagraph(paragraph.id+"-"+occ.resource.uri, new Text(smallContext), List(occ))
                f(annotated)
                ctr += 1
            })
        })
    }
}

object SmallContextOccurrencesCorpus {
    def main(args:Array[String]) {
        new SmallContextOccurrencesCorpus(MilneWittenCorpus.fromDirectory(new File("/home/dirk/Dropbox/eval/wikifiedStories"))).foreach( {
            println
        })
    }
}