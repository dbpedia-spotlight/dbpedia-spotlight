/**
 * Copyright 2011 Pablo Mendes, Max Jakob
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