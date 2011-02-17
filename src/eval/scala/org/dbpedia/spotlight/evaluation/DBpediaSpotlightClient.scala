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

import org.apache.commons.logging.LogFactory
import java.io.{PrintStream, File}
import io.Source
import org.dbpedia.spotlight.annotate.DefaultAnnotator
import org.dbpedia.spotlight.util.AnnotationFilter
import org.dbpedia.spotlight.string.WikiLinkParser
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.{DBpediaResource, DBpediaResourceOccurrence}
/**
 * Reads in manually annotated paragraphs, computes the inter-annotator agreement, then compares
 * our system against the union or intersection of the manual annotators.
 *
 * TODO Create client for Spotlight in the same style created for Alchemy, Ontos, OpenCalais, WikiMachine and Zemanta. i.e. using the Web Service.
 *
 * @author pablomendes
 */
object DBpediaSpotlightClient
{
  private val LOG = LogFactory.getLog(this.getClass)

  val spotterFile    = "/home/pablo/web/TitRedDis.spotterDictionary";
  //val spotterFile    = "/home/pablo/eval/manual/Eval.spotterDictionary";
  val indexDirectory = "/home/pablo/web/DisambigIndex.singleSFs-plusTypes.SnowballAnalyzer.DefaultSimilarity";
  //val indexDirectory = "/home/pablo/web/DisambigIndex.restrictedSFs.plusTypes-plusSFs.SnowballAnalyzer.DefaultSimilarity"
  val confidence = 0.0;
  val support = 0;

  val targetTypesList = null;
  val coreferenceResolution = true;
  val annotator = new DefaultAnnotator(new File(spotterFile), new File(indexDirectory));

  def parseToMatrix(occList : List[DBpediaResourceOccurrence]) : String = {
    val buffer = new StringBuffer();
    var pos = 0;
    var text = "";
    for (occ <- occList) {
      text = occ.context.text;
      val chunk = occ.context.text.substring(pos, occ.textOffset);
      WikiLinkParser.appendToMatrix(chunk, new DBpediaResource(WikiLinkParser.NoTag), buffer);
      WikiLinkParser.appendToMatrix(occ.surfaceForm.name, occ.resource, buffer);
      pos = occ.textOffset + occ.surfaceForm.name.length();
    }
    WikiLinkParser.appendToMatrix(text.substring(pos),new DBpediaResource(WikiLinkParser.NoTag),buffer);
    return buffer.toString();
  }

  def main(args : Array[String])
    {
      
//      val baseDir: String = "/home/pablo/eval/manual"
//      val inputFile: File = new File(baseDir+"AnnotationText.txt");
//      val prefix = "spotlight/AnnotationText-Spotlight";
//      val outputFile: File = new File(baseDir+prefix+".txt.matrix");

//      val baseDir: String = "/home/pablo/eval/cucerzan/"
//      val inputFile: File = new File(baseDir+"cucerzan.txt");

      val baseDir: String = "/home/pablo/eval/manual/"
      val inputFile: File = new File(baseDir+"AnnotationText.txt");

      val prefix = "spotlight/Spotlight";
      val setOutputFile: File = new File(baseDir+prefix+"NoFilter.set");
      val matrixOutputFile = new File(baseDir+prefix+".matrix");
      if (!new File(baseDir).exists) {
        System.err.println("Base directory does not exist. "+baseDir);
        exit();
      }
      
      //ANNOTATE AND TRANSFORM TO MATRIX

      val plainText = Source.fromFile(inputFile).mkString // Either get from file
      //val plainText = AnnotatedTextParser.eraseMarkup(text)     //   or erase markup

      val top10Score = new PrintStream(baseDir+prefix+"Top10Score.set");
      val top10Confusion = new PrintStream(baseDir+prefix+"Top10Confusion.set");
      val top10Prior = new PrintStream(baseDir+prefix+"Top10Prior.set");
      val top10Confidence = new PrintStream(baseDir+prefix+"Top10Confidence.set");
      val top10TrialAndError = new PrintStream(baseDir+prefix+"Top10TrialAndError.set");
      //val out = new PrintStream(matrixOutputFile);
      var occurrences = List[DBpediaResourceOccurrence]();
        for (text <- plainText.split("\n")) {
          val cleanText = WikiLinkParser.eraseMarkup(text);          

          val occList = annotator.annotate(cleanText);
          val occ = occList.toList
          //out.append(parseToMatrix(occ));
          occurrences = occurrences ::: occ;

          //val sixPercent = (cleanText.split("\\s+").size * 0.06).round.toInt;
          val sixPercent = 20;
          val k = Math.min(occ.size, sixPercent)
          
          val entitiesByScore = occ.sortBy(o=>o.similarityScore).reverse.map(o=>o.resource.uri);
          top10Score.append(entitiesByScore.slice(0,k).toSet.mkString("\n"))
          top10Score.append("\n");

          val entitiesByConfusion = occ.sortBy(o=>o.percentageOfSecondRank).map(o=>o.resource.uri); //should not be reversed. small percentage is large gap.
          top10Confusion.append(entitiesByConfusion.slice(0,k).toSet.mkString("\n"))
          top10Confusion.append("\n");

          val entitiesByPrior = occ.sortBy(o=>o.spotProb).reverse.map(o=>o.resource.uri);
          top10Prior.append(entitiesByPrior.slice(0,k).toSet.mkString("\n"))
          top10Prior.append("\n");

          val entitiesByConfidence = occ.sortBy(o=> (o.similarityScore * (1-o.percentageOfSecondRank)) ).reverse.map(o=>o.resource.uri);
          top10Confidence.append(entitiesByConfidence.slice(0,k).toSet.mkString("\n"))
          top10Confidence.append("\n");

          val entitiesByTrialAndError = occ.sortBy(o=> (o.similarityScore * (1-o.percentageOfSecondRank) * o.spotProb) ).reverse.map(o=>o.resource.uri);
          top10TrialAndError.append(entitiesByTrialAndError.slice(0,k).toSet.mkString("\n"))
          top10TrialAndError.append("\n");

        }
      //out.close();
      top10Score.close()
      top10Prior.close()
      top10Confusion.close()
      top10Confidence.close()
      top10TrialAndError.close()


      //val occList : java.util.List[DBpediaResourceOccurrence] = occurrences
      //writeAsEntitySetToFile(occurrences, setOutputFile)
      val allOut = new PrintStream(setOutputFile)
      allOut.append(occurrences.map(occ => occ.resource.uri).toSet.mkString("\n"))
      allOut.close();

//      val top6pc = (0.6 * occurrences.size).toInt
//      val entitiesByPercentageRank = occurrences.map(o => (o.resource,o.percentageOfSecondRank)).distinct.sortBy(t=>t._2);
//      writeAsEntitySetToFile(entitiesByScore.slice(0, top6pc), new File(baseDir+prefix+".sim.t6pc.set"))
//      writeAsEntitySetToFile(entitiesByPercentageRank.slice(0, top6pc), new File(baseDir+prefix+".rank.t6pc.set"))
//      writeAsEntitySetToFile(entitiesByPercentageRank.slice(0,10), new File(baseDir+prefix+".rank.t10.set"))


      val filteredOccList : List[DBpediaResourceOccurrence] = AnnotationFilter.filter(occurrences, 0, 0, AnnotationFilter.DEFAULT_TYPES, "", false, AnnotationFilter.DEFAULT_COREFERENCE_RESOLUTION);
      //val filteredOccList : List[DBpediaResourceOccurrence] = AnnotationFilter.filter(occurrences, 0, 0, AnnotationFilter.PERLOCPLA, AnnotationFilter.DEFAULT_COREFERENCE_RESOLUTION);
      for (confidence <- EvalParams.confidenceInterval) {
          for(support <- EvalParams.supportInterval) {
            var localFiltered = AnnotationFilter.filterBySupport(filteredOccList, support)
            localFiltered = AnnotationFilter.filterByConfidence(localFiltered, confidence)
            val out = new PrintStream(new File(baseDir+prefix+".c"+confidence+"s"+support+".set"))
            out.append(localFiltered.map(occ => occ.resource.uri).toSet.mkString("\n"))
            out.close();
          }
      }

    }

  def writeAsEntitySetToFile(filteredOccList: java.util.List[DBpediaResourceOccurrence], file: File) {
    val out = new PrintStream(file)
    val entitySet = filteredOccList
    out.append(entitySet.map(occ => occ.resource.uri).toSet.mkString("\n"))
    out.close();
  }

    def writeAsEntitySetToFile(occList: List[Tuple2[DBpediaResource,Double]], file: File) {
    val out = new PrintStream(file)
    out.append(occList.map( t => t._1.uri).toSet.mkString("\n"))
    out.close();
  }

}