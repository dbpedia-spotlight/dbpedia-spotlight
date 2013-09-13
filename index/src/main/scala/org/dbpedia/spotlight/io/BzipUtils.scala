package org.dbpedia.spotlight

/**
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.{IOException, BufferedInputStream, FileInputStream, FileOutputStream}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.dbpedia.spotlight.log.SpotlightLog



/**
 * This class is only used for uncompressing the WikipediaDump, but I think this is something we should require for the user to do, or we should patch the DEF's XMLSource to accept InputStream objects as input.
 */
@Deprecated
object BzipUtils {

  def extract(filename: String):String = {
    var buffersize: Int = 1024
    var out: FileOutputStream = null
    var bzIn: BZip2CompressorInputStream = null
    var tempFileName = filename.split("/")

    var newFilename = "/tmp/" + tempFileName.last.replace(".bz2","")
    SpotlightLog.info(this.getClass, "Extracting compressed file into %s...", newFilename)
    try {
      val fin: FileInputStream = new FileInputStream(filename)
      val in: BufferedInputStream = new BufferedInputStream(fin)
      out = new FileOutputStream(newFilename)
      bzIn = new BZip2CompressorInputStream(in, true)
      val buffer: Array[Byte] = new Array[Byte](buffersize)
      var n: Int = 0
      while (-1 != ({ n = bzIn.read(buffer); n })) {
        out.write(buffer, 0, n)
      }
    }
    catch {
      case e: IOException => {
        SpotlightLog.error(this.getClass, e.getMessage)
        e.printStackTrace()
      }
    }
    finally {
      if (out != null) {
        try {
          out.close
        }
        catch {
          case e: IOException => {
               SpotlightLog.error(this.getClass, "Trying close FileOutputStream... ")
               e.printStackTrace();
          }
        }
      }
      if (bzIn != null) {
        try {
          bzIn.close
        }
        catch {
          case e: IOException => {
               SpotlightLog.error(this.getClass, "Trying close BZip2CompressorInputStream... ")
               e.printStackTrace();
          }
        }
      }
    }

    newFilename

  }

}
