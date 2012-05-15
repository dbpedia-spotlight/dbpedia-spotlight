package org.dbpedia.spotlight.util

import java.io._
import org.dbpedia.spotlight.exceptions.{ConfigurationException, InputException}
import scala.xml.{NodeSeq, Node}
import org.apache.commons.logging.LogFactory
import scala._
import scala.Predef._
import org.dbpedia.spotlight.SpotlightController
import org.apache.commons.io.FileUtils

/**
 * @author Joachim Daiber
 *
 */

object ConfigUtil {

  private val log = LogFactory.getLog(classOf[SpotlightController])

  val INJAR_IDENTIFIER = "in_jar"

  def parameter[T](path: Seq[String], base: Node, default: Node): T = nodeAtPath(path, base) match {
    case Some(node) => {
      log.info("Using %s: %s".format(path.mkString("/"), node.toString()))
      node.asInstanceOf[T]
    }
    case None => nodeAtPath(path, default) match {
      case Some(node) => {
        log.info("Using default for %s: %s".format(path.mkString("/"), node.toString()))
        node.asInstanceOf[T]
      }
      case None => throw new ConfigurationException("Parameter %s not specified in either configuration or default configuration.")
    }
  }

  private def nodeAtPath(path: Seq[String], base: Node): Option[Node] = {
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

    if((fileNode \ "type").text equals INJAR_IDENTIFIER ) {
      //Not supported at the moment, but here, we could copy the file
      //to a temporary folder if necessary.
      new File(fileNode.text)
    } else {
      new File(fileNode.text)
    }

  implicit def inputStreamFromNode(fileNode: Node): InputStream =
    if((fileNode \ "type").text equals INJAR_IDENTIFIER ) {
      this.getClass.getResourceAsStream(fileNode.text)
    } else {
      new FileInputStream(new File(fileNode.text))
    }

  implicit def stringFromNode(node: Node): String = node.text
  implicit def intFromNode(node: Node): Int = node.text.toInt
  implicit def floatFromNode(node: Node): Float = node.text.toFloat

}
