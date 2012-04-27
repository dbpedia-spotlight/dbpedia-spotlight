package org.dbpedia.spotlight.util

import xml.Node
import java.io.{FileNotFoundException, FileOutputStream, InputStream, File}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

object ConfigUtil {

  def sanityCheck(conf: Node): Boolean = {
    conf \\ "file" foreach (confFile => {
      if (!file(confFile.text).exists()) {
        throw new FileNotFoundException( "Could not read file defined in %s: %s".format(confFile \ "@name", confFile.text) )
      }
    })
    true
  }

  def file(fileName: String): File = if(resourceBased) {
    val tempFile: Any = File.createTempFile()
    val in: InputStream = this.getClass.getResourceAsStream(fileName)
    val dest = new FileOutputStream(tempFile).getChannel
    dest.transferFrom(in, 0, in.size());

    tempFile
  } else {
    new File(fileName)
  }

}
