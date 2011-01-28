package org.dbpedia.spotlight.evaluation

import external.WikiMachineClient
import java.io.{PrintStream, File}
import io.Source
import org.dbpedia.spotlight.string.WikiLinkParser
/**
 * Generates files in token matrix format. It is basically a TSV file with on pair (token, spotlight) per line.
 * Takes as command line argument a base directory to look for files.
 * TODO Takes as argument a list of files and converts one by one.
 * These files will be used in InterAnnotatorAgreement.R
 * 
 * @author pablomendes
 */

class GenerateMatricesForAgreement {

    def main(args : Array[String])
    {

      val baseDir: String = args(0)+"/"
      if (!new File(baseDir).exists) {
        System.err.println("Base directory does not exist. "+baseDir);
        exit();
      }

      //TRANSFORM THE MANUAL ANNOTATIONS TO MATRIX      
      val testFileNames = List(
          baseDir+"AnnotationText-Pablo.txt",
          baseDir+"AnnotationText-Max.txt",
          baseDir+"AnnotationText-Andres.txt",
          baseDir+"AnnotationText-Paul.txt")
      for(testFileName <- testFileNames) {
        val text = Source.fromFile(testFileName).mkString
        val out = new PrintStream(testFileName+".matrix");
        out.append(WikiLinkParser.parseToMatrix(text));
        out.close();
      }

      //TRANSFORM THE WIKI MACHINE OUTPUT TO MATRIX
      //TODO this snippet below should be inside WikiMachineClient.fromFile(wikiMachineFileName)
      val wikiMachineFileName = baseDir+"AnnotationText-WikiMachine.txt";
      val text = Source.fromFile(wikiMachineFileName).mkString
      val out = new PrintStream(wikiMachineFileName+".matrix");
      out.append(WikiMachineClient.Parser.parseToMatrix(text));
      out.close();



  }

}