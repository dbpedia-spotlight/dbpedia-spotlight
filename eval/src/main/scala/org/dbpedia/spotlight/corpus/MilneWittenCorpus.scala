package org.dbpedia.spotlight.corpus

import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.model._
import java.io.{File, IOException}
import collection.JavaConversions._
import org.xml.sax.InputSource
import io.Source
import xml.NodeSeq
import xml.NodeSeq._
import org.dbpedia.spotlight.spot.WikiMarkupSpotter
import org.dbpedia.spotlight.string.ParseSurfaceFormText

/**
 * Occurrence source for reading the corpus from the original Milne&Witten paper (Wikify system).
 *
 * Choices:
 * - could have one AnnotatedParagraph per HTML file, or one per <p> in those files.
 *
 * @author pablomendes
 */

class MilneWittenCorpus(val documents: Traversable[NodeSeq]) extends AnnotatedTextSource {

    override def foreach[U](f : AnnotatedParagraph => U) {

        documents.foreach( doc => {
            val docId = (doc \\ "title").text
            var i = 0
            val paragraphs = doc.map( _ \\ "p" ).map( _.text.trim )
            paragraphs.foreach( p => {
                i = i + 1
                val paragraphId = docId.concat("-").concat(i.toString)
                val (wikiLinks,cleanText) = parse(p)
                val text = new Text(cleanText)
                val occurrences = wikiLinks.map( w => {
                    val resource = w._1
                    val surfaceForm = w._2
                    val offset = w._3
                    val confidence = w._4
                    new DBpediaResourceOccurrence(paragraphId.concat("-").concat(offset.toString),
                        resource,
                        surfaceForm,
                        text,
                        offset,
                        Provenance.Manual,
                        confidence)
                }).toList
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
        var tail = ""
        val spots = (wikiLinkMatcher findAllIn p).matchData map { m => {  // println(m.start, m.end, m.before, m.after, m.source)
            val wikiLinkString = m.toString //Example: [[The Guardian (Nigeria)|Guardian newspaper|0.4]])
            val wikiLinkArray = wikiLinkString.replaceAll("""\[\[|\]\]""","").split("\\|")
            val uri = wikiLinkArray(0)
            val cleanSurfaceForm = if (wikiLinkArray.length>1) wikiLinkArray(1) else wikiLinkArray(0)
            val confidence = if (wikiLinkArray.length>2) wikiLinkArray(2).toDouble else 1.0
            val lengthDifference = wikiLinkString.length - cleanSurfaceForm.length
            val offset = m.start - accumulatedLengthDifference // this is where the clean surface form starts in the clean text
            //                println(cleanSurfaceForm+"="+cleanText.toString.substring(offset,offset+cleanSurfaceForm.length))
            if (offset equals 0) {
                cleanText.append(m.before)
            } else {
                cleanText.append(m.before.toString.substring(lastDirtyOffset,m.start))
            }
            cleanText.append(cleanSurfaceForm)

            lastDirtyOffset = m.end
            tail = m.after.toString
            println(tail)
            accumulatedLengthDifference = accumulatedLengthDifference + lengthDifference

            (new DBpediaResource(uri),new SurfaceForm(cleanSurfaceForm),offset,confidence.toDouble)
        }}
        cleanText.append(tail)

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
        val dir = new File("/home/pablo/eval/wikify/original")
        MilneWittenCorpus.fromDirectory(dir).filter( o => o.id.contains("APW19980603_0791") ).foreach( {
            println
        })
    }

    def test(dir: File) = {
        val files = dir.listFiles.filter(f => """.*\.htm$""".r.findFirstIn(f.getName).isDefined)
        //files.foreach(println)
        val root = scala.xml.parsing.XhtmlParser(Source.fromFile("/home/pablo/eval/wikify/original/APW19980625_1136.htm"))
        val paragraphs = root.map( _ \ "body" \ "p" ).map( _.text.trim )

        val wikiLinkMatcher = """\[\[(.*?)\]\]""".r

        paragraphs.foreach( p => {
            val cleanText = new StringBuffer()
            var accumulatedLengthDifference = 0
            var lastDirtyOffset = 0
            (wikiLinkMatcher findAllIn p).matchData foreach { m => {  // println(m.start, m.end, m.before, m.after, m.source)

                val wikiLinkString = m.toString //Example: [[The Guardian (Nigeria)|Guardian newspaper|0.4]])
                val wikiLinkArray = wikiLinkString.replaceAll("""\[\[|\]\]""","").split("\\|")
                val uri = wikiLinkArray(0)
                val cleanSurfaceForm = if (wikiLinkArray.length>1) wikiLinkArray(1) else wikiLinkArray(0)
                val confidence = if (wikiLinkArray.length>2) wikiLinkArray(2).toDouble else 1.0
                val lengthDifference = wikiLinkString.length - cleanSurfaceForm.length
                val offset = m.start - accumulatedLengthDifference // this is where the clean surface form starts in the clean text
//                println(cleanSurfaceForm+"="+cleanText.toString.substring(offset,offset+cleanSurfaceForm.length))
                if (offset equals 0) {
                    cleanText.append(m.before)
                } else {
                    cleanText.append(m.before.toString.substring(lastDirtyOffset,m.start))
                }
                cleanText.append(cleanSurfaceForm)

                lastDirtyOffset = m.end
                accumulatedLengthDifference = accumulatedLengthDifference + lengthDifference

                (uri,cleanSurfaceForm,offset,confidence)
            }}
            println(cleanText)
        });

    }
}