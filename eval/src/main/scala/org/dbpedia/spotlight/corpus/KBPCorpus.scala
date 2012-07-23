package org.dbpedia.spotlight.corpus

import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.model._
import java.io.{FileNotFoundException, File}
import xml.XML
import io.Source
import org.apache.commons.logging.LogFactory

/**
 * Created with IntelliJ IDEA.
 * User: Hector
 * Date: 7/23/12
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
 */

class KBPCorpus(val queryFile:File, val answerFile:File, val sourceDir:File, val kbDir:File) extends AnnotatedTextSource {
  override def name = "KBP"
  private val LOG = LogFactory.getLog(this.getClass)

  val queryMap = queryFromFile()
  val sourceDocs = queryMap.values.toList.sorted
  val answers = goldenStandardFromFile()

  val nwFolders = Map[String,String](
    "AFP_ENG" -> "2009/nw/afp_eng",
    "APW_ENG" -> "2009/nw/apw_eng",
    "CNA_ENG" -> "2009/nw/cna_eng",
    "LTW_ENG" -> "2009/nw/ltw_eng",
    "NYT_ENG" -> "2009/nw/nyt_eng",
    "REU_ENG" -> "2009/nw/reu_eng",
    "XIN_ENG" -> "2009/nw/xin_eng"
  )

  val wbFolders = Map[String,String](
    "2009"-> "2009/wb/",
    "2010"-> "2010/wb/"
  )

  val ext = ".sgm"

  def queryFromFile () = {
    val queries= XML.loadFile(queryFile)
    val queryMap = (queries \ "query").map(q => ((q \"@id").text,((q \"name").text,(q \"docid").text))).toMap
    queryMap
  }

  def goldenStandardFromFile() = {
    val answers = Source.fromFile(answerFile).getLines().map(line => line.split("\t")).map(arr => (arr(0),arr(1))).toMap
    answers
  }


  def foreach[U](f: (AnnotatedParagraph) => U) {
    queryMap.foreach{case(qid,(sf,docid)) =>{
      val res = entityIdToRes(answers(qid))
      val paralist = getContextParagraphs(docid,sf)
      paralist.foreach{case (paraText,paraNum,offset)=>{
        val paraId = docid + "-" + paraNum
        val occurrence = new DBpediaResourceOccurrence(
          paraId +"-"+offset,
          res,
          new SurfaceForm(sf),
          paraText,
          offset,
          Provenance.Manual,
          1.0
        )
        val annotated = new AnnotatedParagraph(paraId,paraText,List(occurrence))
        f(annotated)
      }}
    }}
  }

  //TODO: Need to make it efficient to do this
  private def entityIdToRes(eid:String):DBpediaResource = {
    null
  }

  private def parseNews(sf:String, file:File):List[(Text,Int,Int)] ={
    val root = XML.loadFile(file)
    val paragraphs = root \ "P"
    parse(paragraphs.map(n => n.text),sf)
  }

  private def parseWebBlog(sf:String, file:File):List[(Text,Int,Int)] = {
    val root = XML.loadFile(file)
    val body = root \ "POST"
    val paragraphs = body.text.split("\n")
    parse(paragraphs,sf)
  }

  private def parse(paras:Seq[String],sf:String) = {
    var pIdx = 0
    val occList = paras.foldLeft(List[(Text,Int,Int)]())((occList,p) =>{
      val para = p
      val offset = para.indexOf(sf)
      var newList = occList
      if (offset >= 0){
        val context = new Text(para)
        newList = occList :+ (context,pIdx,offset)
      }
      pIdx += 1
      newList
    })
    occList
  }

  private def getContextParagraphs(docId:String,sf:String):List[(Text,Int,Int)] = {
    var paras = List[(Text,Int,Int)]()
    val folderId = docId.split(".")(0)
    val baseDir = sourceDir.getCanonicalPath
    nwFolders.foreach{case(id,path) => {
      if (folderId.startsWith(id)){
         val fullpath = baseDir + "/" + path + "/" + folderId.slice(8,folderId.length-1) +"/"+docId+ext
         try{
           paras = parseNews(sf,new File(fullpath))
         }catch{
           case fofe:FileNotFoundException => LOG.error("The source file path was not found!")
         }
      }
    }}

    wbFolders.foreach{case(year,path) =>{
      val wbFile = new File(baseDir + "/" + path + "/" + docId+ext)
      if (wbFile.exists) {
        paras = parseWebBlog(sf,wbFile)
      }
    }}
    paras
  }
}

object KBPCorpus {
  val qFile = new File("D:\\Hector\\Downloads\\TAC_2010_KBP_Training_Entity_Linking_V2.0\\data/tac_2010_kbp_training_entity_linking_queries.xml")
  val aFile = new File("D:\\Hector\\Downloads\\TAC_2010_KBP_Training_Entity_Linking_V2.0\\data/tac_2010_kbp_training_entity_linking_query_types.tab")
  val sourceDir = new File("D:\\nlp\\kbp\\2010_data/TAC_2010_KBP_Source_Data")
  val kbDir = new File("D:\\nlp\\kbp\\2009_data\\data")

  val kbp = new KBPCorpus (qFile,aFile,sourceDir,kbDir)

  def main (args: Array[String]){

  }
}
