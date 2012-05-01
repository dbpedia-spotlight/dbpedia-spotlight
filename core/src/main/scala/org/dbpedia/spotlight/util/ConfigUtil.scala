package org.dbpedia.spotlight.util

import java.io._
import org.dbpedia.spotlight.exceptions.{ConfigurationException, InputException}
import xml.{NodeSeq, Node}

/**
 * @author Joachim Daiber
 *
 */

object ConfigUtil {

  val RESOURCE_IDENTIFIER = "in_jar"

  def parameter[T](path: List[String], base: Node, default: Node): T = nodeAtPath(path, base) match {
    case Some(node) => node.asInstanceOf[T]
    case None => nodeAtPath(path, default).getOrElse({
      throw new ConfigurationException("Required parameter not specified in configuration file.")
    }).asInstanceOf[T]
  }

  private def nodeAtPath(path: List[String], base: Node): Option[Node] = {
    var node: Node = base
    path.dropRight(1).foreach( pathSegment => {
      node = (node \ pathSegment).head
    })
    (node \ "_") foreach( node => {
      if ((node \ "@name").text equals path.tail) {
        return Some(node)
      }
    })
    None
  }

  def defaultNode(node: Node, default: Node): Node = {
    val id: String = (node \ "@id").text
    ((default \ "_") filter (innerNode => {(innerNode \ "@id").text equals id })).head
  }

  def sanityCheck(conf: Node): Boolean = {
    conf \ "file" foreach (confFile => {
      if (!fileFromNode(confFile).exists()) {
        throw new FileNotFoundException( "Could not read file defined in %s: %s".format(confFile \ "@name", confFile.text) )
      }
    })
    true
  }

  /*******************************************************************
   *  Conversions from XML node to Scala/Java data types             *
   *******************************************************************/

  implicit def fileFromNode(fileNode: Node): File =

    if((fileNode \ "type").text equals RESOURCE_IDENTIFIER ) {
      val tempFile: File = File.createTempFile("a", "b")
      val in: InputStream = this.getClass.getResourceAsStream(fileNode.text)

      val dest = new FileOutputStream(tempFile).getChannel
      dest.transferFrom(in, 0, in.size());

      tempFile
    } else {
      new File(fileNode.text)
    }

  implicit def inputStreamFromNode(fileNode: Node): InputStream =
    if((fileNode \ "type").text equals RESOURCE_IDENTIFIER ) {
      this.getClass.getResourceAsStream(fileNode.text)
    } else {
      new FileInputStream(new File(fileNode.text))
    }

  implicit def stringFromNode(node: Node): String = node.text
  implicit def intFromNode(node: Node): Int = node.text.toInt
  implicit def floatFromNode(node: Node): Float = node.text.toFloat

}
