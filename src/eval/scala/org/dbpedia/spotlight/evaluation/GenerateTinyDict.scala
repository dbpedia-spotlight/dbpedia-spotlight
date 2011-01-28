package org.dbpedia.spotlight.evaluation

import java.io.{PrintStream, File}
import io.Source
import org.dbpedia.spotlight.spot.lingpipe.IndexLingPipeSpotter
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence}
import org.dbpedia.spotlight.string.WikiLinkParser

/**
 * @author pablomendes
 */

class GenerateTinyDict   {

  val spotterFile    = "e:\\dbpa\\web\\Eval.spotterDictionary";
  
  def main(args : Array[String])
    {

      val baseDir: String = args(0)+"/"
      if (!new File(baseDir).exists) {
        System.err.println("Base directory does not exist. "+baseDir);
        exit();
      }

      //EXTRACT OCCURRENCES FROM FILES AND WRITE THEM OUT TO A DICTIONARY FOR TESTING
      var occList = List[DBpediaResourceOccurrence]();
      val testFileNames = List(
          baseDir+"AnnotationText-Pablo.txt",
          baseDir+"AnnotationText-Max.txt",
          baseDir+"AnnotationText-Andres.txt",
          baseDir+"AnnotationText-Paul.txt")
      for(testFileName <- testFileNames) {
        val text = Source.fromFile(testFileName).mkString
        occList = occList ::: WikiLinkParser.parse(text).toList;
      }
      // WRITE OUT A MINI DICTIONARY FOR TESTING
      val dictionary = IndexLingPipeSpotter.getDictionary(occList);
      IndexLingPipeSpotter.writeDictionaryFile(dictionary,new File(spotterFile));

    }
}