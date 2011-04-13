package org.dbpedia.spotlight.candidate

import io.Source
import com.officedepot.cdap2.collection.CompactHashSet
import java.io._
import org.dbpedia.spotlight.model.SurfaceFormOccurrence
import collection.JavaConversions._
import org.apache.commons.logging.LogFactory

/**
 * This is a temporary workaround to the common words problem. Pablo is working on the actual fix.
 *
 * @author pablomendes
 */
class CommonWordFilter(val filename: String) extends SpotSelector {

    val extension = ".CompactHashSet";
    val minimumCount = 100;
    val file = new File(filename+extension);

    val commonWords = if (file exists) unserialize else { serialize(parse); unserialize }

    private val LOG = LogFactory.getLog(this.getClass)

    def parse() = {
        LOG.info(" parsing common words file ")
        // get lines, split in three fields, get the middle one (word)
        val commonWords = new CompactHashSet[String]();

        val log = Source.fromFile(filename, "iso-8859-1").getLines.foreach(line => {
            if (line.trim()!="") {
                val fields = line.split("\\s")
                if (fields(2).toInt > minimumCount) commonWords.add(fields(1))
            }
        });
        commonWords
    }

    def serialize(commonWords: CompactHashSet[String]) {
        LOG.info(" saving common words dictionary to disk ")
        val out = new ObjectOutputStream(new FileOutputStream(file))
        out.writeObject(commonWords)
        out.close()
    }

    def unserialize() = {
        LOG.info(" loading serialized dictionary of common words ")
        val in = new ObjectInputStream(new FileInputStream(file))
        val set = in.readObject match {
            case s: CompactHashSet[String] => s
            case _ => throw new ClassCastException("Serialized Object is not of type CompactHashSet[String] ");
        }
        in.close();
        set
    }

    def isCommonWord(occ: SurfaceFormOccurrence) = {
        commonWords contains occ.surfaceForm.name
    }

    /**
     * Select only spots that are not common words (this is a workaround)
     */
    def select(occs: java.util.List[SurfaceFormOccurrence]) : java.util.List[SurfaceFormOccurrence] = {
        val previousSize = occs.size
        val r = occs.filter( o => !isCommonWord(o) );
        val count = previousSize-r.size
        val percent = if (count==0) 0 else count.toDouble / previousSize
        LOG.info(String.format("Removed %s occurrences confusable with common words (%s\\%).", count.toString, percent.toString ))
        r
    }

    def main(args: Array[String]) {
        def usage = println(" Usage: scala -cp $CP CommonWordFilter words.txt ")
        args(0) match {
            case file: String => {
                new CommonWordFilter(file)
            }
            case _ => usage
        }
    }

}

