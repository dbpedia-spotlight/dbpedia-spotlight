package org.dbpedia.spotlight.corpus

import org.dbpedia.spotlight.io.AnnotatedTextSource
import java.io.File
import io.Source
import collection.mutable.HashMap
import xml.{NodeSeq, XML}
import org.dbpedia.spotlight.model._

/**
 * @author Joachim Daiber
 */

class CSAWCorpus(val texts: HashMap[String, Text], val annotations: NodeSeq) extends AnnotatedTextSource {

  override def name = "CSAW"

  override def foreach[U](f : AnnotatedParagraph => U) {
    val occs  = HashMap[String, List[DBpediaResourceOccurrence]]()

    annotations \ "annotation" foreach (annotation => {
      val docName  = (annotation \ "docName").text
      val wikiName = (annotation \ "wikiName").text
      val offset   = (annotation \ "offset").text.toInt
      val length   = (annotation \ "length").text.toInt

      if(docName != null && wikiName != null && !wikiName.equals("")){
        val text = texts(docName)
        val sf  = new SurfaceForm(text.text.substring(offset, offset+length))
        val res = new DBpediaResource(wikiName)
        occs(docName) = occs.getOrElse(docName, List()) ::: List(new DBpediaResourceOccurrence(res, sf, text, offset))
      }
    })

    occs.keys.foreach( doc => {
      f(new AnnotatedParagraph(doc, texts(doc), occs(doc)))
    })
  }
}

object CSAWCorpus {

  def fromDirectory(directory: File) = {
    val texts = HashMap[String, Text]()
    new File(directory, "crawledDocs").listFiles.filter(f => !(f.getName.startsWith(".") || f.getName.equals("CZdata1") || f.getName.equals("docPaths.txt") || f.getName.equals("13Oct08_allUrls.txt.txt")) ) foreach(
      crawledDoc => {
        texts.put(crawledDoc.getName, new Text(Source.fromFile(crawledDoc).mkString))
      }
    )
    new CSAWCorpus(texts, XML.loadFile(new File(directory, "CSAW_Annotations.xml")))
  }

  def main (args: Array[String]) {
    val dir = new File("/Users/jodaiber/Desktop/original")
    CSAWCorpus.fromDirectory(dir).foreach( p => {
      println(p)
    })
  }
}

