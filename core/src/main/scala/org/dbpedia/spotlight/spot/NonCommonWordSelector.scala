package org.dbpedia.spotlight.spot

import io.Source
import com.officedepot.cdap2.collection.CompactHashSet
import java.io._
import org.dbpedia.spotlight.model.SurfaceFormOccurrence
import collection.JavaConversions._
import org.apache.commons.logging.Log
import org.apache.log4j.Logger
import org.dbpedia.spotlight.io.WortschatzParser


/**
 * This is a temporary workaround to the common words problem.
 * It works as a blacklist of surface forms.
 * It currently packs also a parser and a serializer besides the filter.
 * TODO factor out the parser/serializer to a class in io or util.
 *
 * See also: SurfaceFormWhitelistSelector
 *
 * @author pablomendes
 */
class NonCommonWordSelector(val filename: String, val load: Boolean = true) extends UntaggedSpotSelector {

    val LOG = Logger.getLogger(this.getClass);

    val extension = ".CompactHashSet";
    val file = new File(filename+extension);

    val commonWords = if (load && (file exists)) unserialize else { serialize(parse); unserialize }

    def parse() = {
        WortschatzParser.parse(filename, 100)
    }

    def serialize(commonWords: CompactHashSet[String]) {
        LOG.info(" saving common words dictionary to disk ")
        val out = new ObjectOutputStream(new FileOutputStream(file))
        out.writeObject(commonWords)
        out.close()
    }

    def unserialize = {
        LOG.info(" loading serialized dictionary of common words ")
        var set = new CompactHashSet[String]
        try {
            val in = new ObjectInputStream(new FileInputStream(file))
            set = in.readObject match {
                case s: CompactHashSet[String] => s
                case _ => throw new ClassCastException("Serialized Object is not of type CompactHashSet[String] ");
            }
            in.close();
        } catch {
            case e: Exception => LOG.info("Error loading CommonWords. "+e.getMessage);
        }
        set
    }

    def isCommonWord(occ: SurfaceFormOccurrence) = {
        commonWords contains occ.surfaceForm.name
    }

    /**
     * Select only spots that are not common words (this is a workaround)
     */
    def select(occs: java.util.List[SurfaceFormOccurrence]) : java.util.List[SurfaceFormOccurrence] = {
        occs.filter( o => !isCommonWord(o) );
    }

    def main(args: Array[String]) {
        def usage = println(" Usage: scala -cp $CP NonCommonWordSelector words.txt ")
        args(0) match {
            case file: String => {
                new NonCommonWordSelector(file)
            }
            case _ => usage
        }
    }

}
