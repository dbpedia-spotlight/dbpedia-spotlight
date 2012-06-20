package org.dbpedia.spotlight.corpus

import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.model._
import java.io.File
import io.Source
import xml.NodeSeq
/**
 * Occurrence source for reading the corpus from the original Milne&Witten paper (Wikify system).
 *
 * Choices:
 * - could have one AnnotatedParagraph per HTML file, or one per <p> in those files.
 *
 * @author pablomendes
 */

class MilneWittenCorpus(val documents: Traversable[NodeSeq]) extends AnnotatedTextSource {

    override def name = "MilneWitten"

    override def foreach[U](f : AnnotatedParagraph => U) {

        documents.foreach( doc => {
            val docId = (doc \\ "title").text
            var i = 0
            val paragraphs = doc.map( _ \\ "p" ).map( _.text.trim )  // can change here to have one text item per doc or per paragraph
            paragraphs.foreach( p => {
                i = i + 1
                val paragraphId = docId.concat("-").concat(i.toString)
                val (wikiLinks,cleanText) = parse(p)
                val text = new Text(cleanText)
                val occurrences = wikiLinks.map{ case (resource, surfaceForm, offset, confidence) => {
                    new DBpediaResourceOccurrence(paragraphId.concat("-").concat(offset.toString),
                        resource,
                        surfaceForm,
                        text,
                        offset,
                        Provenance.Manual,
                        confidence)
                }}.toList
                val annotated = new AnnotatedParagraph(paragraphId, text, occurrences)
                f(annotated)
            })
        })

    }

    val wikiLinkMatcher = """\[\[(.*?)\]\]""".r
    def parse(p: String) : (List[(DBpediaResource,SurfaceForm,Int,Double)],String) = {
        val cleanText = new StringBuilder()
        var accumulatedLengthDifference = 0
        var lastDirtyOffset = 0
        val matches = (wikiLinkMatcher findAllIn p).matchData.toTraversable
        val nMatches = matches.size
        var i = 0
        val spots = matches.map( m => {  // println(m.start, m.end, m.before, m.after, m.source)
            i = i + 1
            val wikiLinkString = m.toString //Example: [[The Guardian (Nigeria)|Guardian newspaper|0.4]])
            val wikiLinkArray = wikiLinkString.replaceAll("""\[\[|\]\]""","").split("\\|")
            val uri = wikiLinkArray(0)
            val cleanSurfaceForm = if (wikiLinkArray.length>1) wikiLinkArray(1) else wikiLinkArray(0)
            val confidence = if (wikiLinkArray.length>2) wikiLinkArray(2).toDouble else 1.0
            val lengthDifference = wikiLinkString.length - cleanSurfaceForm.length
            val offset = m.start - accumulatedLengthDifference // this is where the clean surface form starts in the clean text

            if (offset equals 0) {
                cleanText.append(m.before)
            } else {
                cleanText.append(m.before.toString.substring(lastDirtyOffset,m.start))
            }
            cleanText.append(cleanSurfaceForm)

            lastDirtyOffset = m.end

            if (i==nMatches)
                cleanText.append(m.after.toString)

            accumulatedLengthDifference = accumulatedLengthDifference + lengthDifference
            
            (new DBpediaResource(uri),new SurfaceForm(cleanSurfaceForm),offset,confidence.toDouble)
        })

        (spots.toList, cleanText.toString)
    }
}

object MilneWittenCorpus {

    def fromDirectory(directory: File) = {
        val files = directory.listFiles.filter(f => """.*\.htm$""".r.findFirstIn(f.getName).isDefined)
        val elements = files.map( f => scala.xml.parsing.XhtmlParser(Source.fromFile(f)) )
        new MilneWittenCorpus(elements)
    }

    def main (args: Array[String]) {
        val dir = new File(args(0)) // /home/pablo/eval/wikify/original/
        MilneWittenCorpus.fromDirectory(dir).foreach( {
            println
        })
    }

}
