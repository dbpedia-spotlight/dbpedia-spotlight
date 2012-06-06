package org.dbpedia.spotlight.corpus

import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.model._
import java.io.File
import io.Source
import scala.None
import collection.mutable.ListBuffer
import org.dbpedia.spotlight.corpus.AidaCorpus.{CoNLLToken, CoNLLDoc}

/**
 * Occurrence source for reading the corpus from the AIDA System
 *
 *
 * @author pablomendes
 */

class AidaCorpus(val documents: List[CoNLLDoc]) extends AnnotatedTextSource {

    val nilUri = "--NME--"

    override def foreach[U](f : AnnotatedParagraph => U) {

        documents.foreach( doc => {
            val paragraphId = "%s_%s".format(doc.docId,doc.docLabel)
            val cleanText = new StringBuilder()
            val annotations = doc.tokens.flatMap( token => {
                val annotation = if (token.bioTag equals "B") { //beginning of a tagged entity
                    val offset = cleanText.toString().length
                    Some((paragraphId, new DBpediaResource(token.resourceUri), new SurfaceForm(token.surfaceForm), offset))
                } else {
                    None
                }
                cleanText.append(token.token).append(" ")
                annotation
            })
            val occs = annotations.map( annotation => {
                val paragraphId = annotation._1
                val resource = annotation._2
                val surfaceForm = annotation._3
                val offset = annotation._4
                val text = new Text(cleanText.toString)
                new DBpediaResourceOccurrence(paragraphId,
                    resource,
                    surfaceForm,
                    text,
                    offset,
                    Provenance.Manual,
                    1.0)
            }).toList

            val annotated = new AnnotatedParagraph(paragraphId, new Text(cleanText.toString), occs)
            f(annotated)

        })

    }

}

object AidaCorpus {

    class CoNLLDoc(val docId: String, val docLabel: String) {
        val tokens = new ListBuffer[CoNLLToken]()
        override def toString = {
            "(%s %s):".format(docId,docLabel).concat(tokens.toString).concat("\n")
        }
    }

    class CoNLLToken(val token: String, val bioTag: String, val surfaceForm: String, val resourceUri: String, val resourceType: String) {
        override def toString = {
            "%s".format(token)//,bioTag,surfaceForm,resourceUri,resourceType)
        }
    }

    def parseLines(lines: Iterator[String]) : List[CoNLLDoc] = {
         /*
        -DOCSTART- (3 China)
        China	B	China	People\u0027s_Republic_of_China	LOCATION
        */
        val NewDocMarker = """^-DOCSTART- \((\d+)(.+)\)""".r
        val TokenDescription = """^(\S+)\t(\S+)\t(.+)\t(\S+)\t(\S+)$""".r

        var newDoc = new CoNLLDoc("","")
        lines.flatMap(line => {
//            println(line);
            line match {
                case NewDocMarker(docId,docLabel) => {
                    //println("new doc (%s %s)".format(docId,docLabel))
                    val currentDoc = newDoc
                    newDoc = new CoNLLDoc(docId,docLabel.trim.replaceAll(" ","_"))
                    if (currentDoc.docId.isEmpty)
                        None
                    else
                        Some(currentDoc)
                }
                case TokenDescription(token,bioTag,surfaceForm,resourceUri,resourceType) => {
                    //println("%s -> annotated".format(token))
                    newDoc.tokens += new CoNLLToken(token,bioTag,surfaceForm,resourceUri,resourceType)
                    None
                }
                case _ => {
                    //println("%s -> plain".format(line))
                    newDoc.tokens += new CoNLLToken(line.trim,"","","","")
                    None
                }
            }
        }).toList ++ List(newDoc)
    }


    def fromFile(file: File) = {
        val lines = Source.fromFile(file).getLines
        new AidaCorpus(parseLines(lines))
    }


    def parse(line: String) = {
        line.split("\t")
    }

    def main (args: Array[String]) {
        val file = new File("/home/pablo/eval/aida/gold/CoNLL-YAGO.tsv") //args(0))

        AidaCorpus.fromFile(file).foreach( {
            println
        })
    }

}
