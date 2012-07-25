package org.dbpedia.spotlight.topic.util

import xml.XML
import org.dbpedia.spotlight.model.Topic
import java.io.{FileWriter, PrintWriter}
import scala.util.control.Breaks._
import scala.collection.mutable._


/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 7/3/12
 * Time: 3:06 PM
 * To change this template use File | Settings | File Templates.
 */

object ExtractIptcTopics {

  def main(args:Array[String]) {
    val pathToIptc = "/media/Data/Wikipedia/iptc.xml"
    val pathToOutput = "/media/Data/Wikipedia/topics.list"

    val pw = new PrintWriter(new FileWriter(pathToOutput))

    val iptcXml = XML.loadFile(pathToIptc)

    class Score(var score:Int) { }

    var iptcHierarchy = Map[String,(String,Score,String)]()

    val concepts = iptcXml \\ "concept"

    breakable { concepts.foreach( concept => {
      val conceptId = ((concept \\ "conceptId").head \\ "@qcode").head.text
      var conceptName = (concept \\ "name").head.text
      conceptName = conceptName.replaceAll("([^a-zA-Z]|and)+","_")
      conceptName = conceptName.replaceAll("__","_")
      val broader =  concept \\ "broader"
      if (broader.length > 0) {
        val parent = (broader.head \\ "@qcode").head.text

        iptcHierarchy += (conceptId -> ((conceptName,
          {if (iptcHierarchy.getOrElse(parent,(null,Int.MaxValue,null))._2.equals(Int.MaxValue))
            new Score(Int.MaxValue)
          else
            new Score(iptcHierarchy(parent)._2.score+1) }
          , parent )))
      }
      else
        iptcHierarchy += (conceptId -> ((conceptName,new Score(0), "" )) )

    }) }

    var done = false
    while(!done) {
      done = true
      iptcHierarchy.foreach { case (child,info) => {
        if (info._2.score.equals(Int.MaxValue))
          if (iptcHierarchy(info._3)._2.score.equals(Int.MaxValue))
            done = false
          else
            info._2.score = iptcHierarchy(info._3)._2.score+1
      }}
    }

    iptcHierarchy.foreach { case (child,info) => {
      if(info._2.score<2)
        pw.println(info._1+"\t"+child+"\t"+info._3)
    }}
    pw.close()
  }

}
