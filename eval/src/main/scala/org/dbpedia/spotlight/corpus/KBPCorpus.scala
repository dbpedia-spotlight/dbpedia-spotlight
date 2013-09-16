package org.dbpedia.spotlight.corpus

import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.model._
import java.io.File
import xml.XML
import io.Source
import collection.mutable.ListBuffer
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.spotlight.log.SpotlightLog

/**
 * Created with IntelliJ IDEA.
 * User: Hector
 * Date: 7/23/12
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * This class allow reading KBPCorpus as AnnotatedTextSource
 * There are many files to prepared to get a KBPCorpus, see the parameters below
 * Please note that query files and answer files should correspond to each other
 * @param queryFile The entity linking queries XML file in KBP data
 * @param answerFile The annotation (entity_linking_query_types.tab) file in KBP data
 * @param sourceDir  The extraced TAC_2010_Source_Data/data directory, containing source newswires and web blogs
 * @param kbDir The extraced Knowledge Base/data Directory
 */
class KBPCorpus(val queryFile:File, val answerFile:File, val sourceDir:File, val kbDir:File) extends AnnotatedTextSource {
  override def name = "KBP"

  //preparing queries
  val queryMap = queryFromFile()
  //preparing answers(golden standards)
  val answers = goldenStandardFromFile()
  //preparing knowledge base
  val kb = kbFromDirectory()

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
    val content = Source.fromFile(queryFile).mkString
    val fixed = content.replaceAll("(?i)encoding=\"utf8\"","encoding=\"utf-8\"") //fix encoding to make SAXpaser accept the document
    val queries= XML.loadString(fixed)
    val queryMap = (queries \ "query").map(q => ((q \"@id").text,((q \"name").text,(q \"docid").text))).toMap
    queryMap
  }

  def goldenStandardFromFile() = {
    val answers = Source.fromFile(answerFile).getLines().map(line => line.split("\t")).map(arr => (arr(0),arr(1))).toMap
    answers
  }

  //assume that entity indices are strictly increasing
  def kbFromDirectory() = {
    SpotlightLog.info(this.getClass, "Loading Knowledge Base from directory")
    val uriList = new ListBuffer[String]
    val files = kbDir.listFiles.filter(f => """.*\.xml$""".r.findFirstIn(f.getName).isDefined).sorted //knowledge base files are alphabetically ordered
    var lastId = 0
    var entityCount = 0
    var skippedCount = 0
    files.foreach(f =>{
       val root = XML.loadFile(f)
       val entities = root \ "entity"
       entityCount += entities.length

       entities.foreach(e => {
         val idStr = e.attribute("id").get.toString()
         val id = idStr.slice(1,idStr.length).toInt

         val idGap = id - lastId - 1
         skippedCount += idGap

         (1 to idGap).foreach(_ => uriList+="") //the missed entity id is treated as empty uri

         lastId = id
         uriList += WikiUtil.wikiEncode(e.attribute("wiki_title").getOrElse("").toString())
       })

    })

    SpotlightLog.info(this.getClass, "Done. Read in %s entities. %s entities skipped in knowledge base (mark as empty uri). Max entity index: %s",
              entityCount.toString, skippedCount.toString,uriList.length.toString)

    if (uriList.length != entityCount+skippedCount)
        SpotlightLog.error(this.getClass, "Read in %s entities, skipped %s entities, but stored %s entities",entityCount.toString,skippedCount.toString,uriList.length.toString)

    uriList.toArray
  }

  /**
   * KBP is query basis.
   * This foreach operates over each queries
   * In each paragraph there is typically only one occurrence
   * @param f function to be applied on each AnnotataedParagraph
   * @tparam U
   */
  def foreach[U](f: (AnnotatedParagraph) => U) {
    queryMap.foreach{case(qid,(sf,docid)) =>{
      val answer = answers(qid)
      if (!answer.startsWith("NIL")){ //now ignore NIL answers
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
      }
    }}
  }

  private def entityIdToRes(eid:String):DBpediaResource = {
    val index = eid.slice(1,eid.length).toInt - 1   //KB index start at 1, must -1
    val res = new DBpediaResource(kb(index))

    if (res.uri == "") SpotlightLog.error(this.getClass, "%s : [%s] cannot be matched with a valid uri in knowledge base",eid,kb(index))

    res
  }

  //assumes the sgml file is well-formed and can be treated as xml
  private def parseNews(sf:String, file:File):List[(Text,Int,Int)] ={
    val root = XML.loadFile(file)
    val paragraphs = root \\ "P"
    val res = parse(paragraphs.map(n => n.text.replace("\n"," ")),sf)

    if(res.length == 0) SpotlightLog.warn(this.getClass, "%s not found in file(news wire): %s", sf, file.getAbsolutePath)

    res
  }

  //assumes the sgml file is well-formed and can be treated as xml
  private def parseWebBlog(sf:String, file:File):List[(Text,Int,Int)] = {
    val root = XML.loadFile(file)
    val body = root \\ "POST"
    val paragraphs = body.text.split("\\n\\n")  //paragraphs are seperated by one additional new line in source files
    val res= parse(paragraphs.map(p => p.replace("\n"," ")),sf)

    if(res.length == 0) SpotlightLog.warn(this.getClass, "%s not found in file(web blog): %s", sf, file.getAbsolutePath)

    res
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
    val folderId = docId.split('.')(0)
    val baseDir = sourceDir.getCanonicalPath
    nwFolders.foreach{case(id,path) => {
      if (folderId.startsWith(id)){
         val fullpath = baseDir + "/" + path + "/" + folderId.slice(8,folderId.length) +"/"+docId+ext
         paras = parseNews(sf,new File(fullpath))
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
  val qFile = new File("/mnt/windows/Extra/Researches/data/kbp/queries/TAC_2011_KBP_English_Evaluation_Entity_Linking_Annotation/data/tac_2011_kbp_english_evaluation_entity_linking_queries.xml")
  val aFile = new File("/mnt/windows/Extra/Researches/data/kbp/queries/TAC_2011_KBP_English_Evaluation_Entity_Linking_Annotation/data/tac_2011_kbp_english_evaluation_entity_linking_query_types.tab")

  //val qFile = new File("/mnt/windows/Extra/Researches/data/kbp/queries/TAC_2009_KBP_Evaluation_Entity_Linking_List/data/entity_linking_queries.xml")
  //val aFile = new File("/mnt/windows/Extra/Researches/data/kbp/queries/TAC_2009_KBP_Gold_Standard_Entity_Linking_Entity_Type_List/data/Gold_Standard_Entity_Linking_List_with_Entity_Types.tab")

  //val qFile = new File("/mnt/windows/Extra/Researches/data/kbp/queries/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.0/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.1/data/tac_2010_kbp_evaluation_entity_linking_queries.xml")
  //val aFile = new File("/mnt/windows/Extra/Researches/data/kbp/queries/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.0/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.1/data/tac_2010_kbp_evaluation_entity_linking_query_types.tab")

  val sourceDir = new File("/mnt/windows/Extra/Researches/data/kbp/kbp2011/TAC_KBP_2010_Source_Data/TAC_2010_KBP_Source_Data/data")
  val kbDir = new File("/mnt/windows/Extra/Researches/data/kbp/kbp2011/TAC_2009_KBP_Evaluation_Reference_Knowledge_Base/data")

  val kbp = new KBPCorpus (qFile,aFile,sourceDir,kbDir)

  def main (args: Array[String]){
    kbp.foreach(println)
  }
}
