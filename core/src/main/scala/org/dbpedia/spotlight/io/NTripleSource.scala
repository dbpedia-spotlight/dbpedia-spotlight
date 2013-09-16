package org.dbpedia.spotlight.io

import scala.io.Source
import java.io._
import java.util.zip.GZIPInputStream
import org.dbpedia.spotlight.log.SpotlightLog

/**
 * Iterates over nt files. Can also iterate over nq files not caring about the fourth element.
 * @author dirk
 */
object NTripleSource {
    def fromFile(ntFile : File) : NTripleSource = new NTripleSource(ntFile)

    class NTripleSource(ntFile: File) extends Traversable[(String,String,String)] {

        override def foreach[U]( f: ((String,String,String)) => U) {
            var input : InputStream = new FileInputStream(ntFile)
            if (ntFile.getName.endsWith(".gz")) {
                input = new GZIPInputStream(input)
            }

            var linesIterator : Iterator[String] = Iterator.empty
            try {
                linesIterator = Source.fromInputStream(input, "UTF-8").getLines
            }
            catch {
                case e: java.nio.charset.MalformedInputException => linesIterator = Source.fromInputStream(input).getLines
            }

            for (line <- linesIterator) {
                if (!line.startsWith("#")) { //comments
                    val elements = line.trim.split(" ")

                    if (elements.length >= 4) {
                        var subj = elements(0)
                        var pred = elements(1)
                        var obj = elements(2)

                        subj = subj.substring(1,subj.length-1)
                        pred = pred.substring(1,pred.length-1)
                        obj = obj.substring(1,obj.length-1)

                        f((subj,pred,obj))
                    }
                    else {
                        SpotlightLog.error(this.getClass, "line must have at least 4 whitespaces; got %d in line: %d", elements.length-1,line)
                    }
                }
            }
        }
    }
}
