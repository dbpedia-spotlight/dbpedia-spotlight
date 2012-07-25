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
 * TODO can be generalized for reading CoNLL NER format.
 *
 * @author pablomendes
 */

class AidaCorpus(val documents: List[CoNLLDoc]) extends AnnotatedTextSource {

    override def name = "AIDA"

    override def foreach[U](f : AnnotatedParagraph => U) {

        documents.foreach( doc => {
            val paragraphId = "%s_%s".format(doc.docId,doc.docLabel)
            val cleanText = new StringBuilder()
            var inside = false
            var currentEntityName = ""
            val annotations = doc.tokens.flatMap( token => {
                val annotation = if (token.bioTag equals "B") { //beginning of a tagged entity
                    inside = true
                    currentEntityName = token.surfaceForm
                    val offset = cleanText.toString().length
                    Some((paragraphId, new DBpediaResource(token.resourceUri), new SurfaceForm(token.surfaceForm), offset))
                } else if (token.bioTag equals "I") {
                    None
                } else {
//                    if (inside)
//                        cleanText.append(currentEntityName).append(" ")
//                    else
//                        cleanText.append(token.token).append(" ")
                    inside = false
                    None
                }
                if (token.token.equals("'s") || //fix tokenization while putting text back together
                    token.token.equals(":") ||
                    token.token.equals(",") ||
                    token.token.equals("!") ||
                    token.token.equals("."))
                    cleanText.delete(cleanText.length-1,cleanText.length).append(token.token).append(" ")
                else
                    cleanText.append(token.token).append(" ")
                annotation
            })
            val text = new Text(cleanText.toString.trim)
            val occs = annotations.map{ case (paragraphId,resource,surfaceForm,offset) => {
                new DBpediaResourceOccurrence(paragraphId+"-"+offset,
                    resource,
                    surfaceForm,
                    text,
                    offset,
                    Provenance.Manual,
                    1.0)
            }}.toList

            val annotated = new AnnotatedParagraph(paragraphId, text, occs)
            f(annotated)

        })

    }

}

object AidaCorpus {

    val nilUri = "--NME--"

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

    implicit def stringToFile(s: String) = new File(s)

    def fromFile(file: File) = {
        val lines = Source.fromFile(file).getLines
        new AidaCorpus(parseLines(lines))
    }


    def parse(line: String) = {
        line.split("\t")
    }

    def main (args: Array[String]) {
        val file = new File(args(0)) //"/home/pablo/eval/aida/gold/CoNLL-YAGO.tsv")

        AidaCorpus.fromFile(file)
            //.filter(_.id.contains("1393"))
            .foreach(println)
    }

}
