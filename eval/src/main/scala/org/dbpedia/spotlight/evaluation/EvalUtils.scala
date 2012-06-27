package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.string.WikiLinkParser
import org.dbpedia.spotlight.model.{DBpediaResource, DBpediaResourceOccurrence}
import org.dbpedia.spotlight.filter.annotations._
import java.io.{File, FileOutputStream, PrintStream}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.extract.TagExtractorFromAnnotator
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Helper functions to parse and write dbpedia resource occurrences during evaluation tasks
 * TODO need more meaningful organization e.g. parseToMatrix can go to WikiLinkParser
 *     -
 * @author pablomendes
 */

object EvalUtils {

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

    def writeResultsForIntervals(baseDir: String, prefix: String, occurrences: List[DBpediaResourceOccurrence], id: String = "", similarityThresholds: List[Double]) {
        //TODO What happens here if we give all occurrences from different texts?
        val filteredOccList = new CoreferenceFilter().filterOccs(occurrences)

        for (confidence <- EvalParams.confidenceInterval) {
            val confidenceFilter = new ConfidenceFilter(similarityThresholds, confidence)
            for(support <- EvalParams.supportInterval) {
                val supportFilter = new SupportFilter(support)

                //var localFiltered = filter.filterBySupport(filteredOccList, support)
                //localFiltered = filter.filterByConfidence(localFiltered, confidence)
                var localFiltered = supportFilter.filterOccs(filteredOccList)
                localFiltered = confidenceFilter.filterOccs(localFiltered)
                val ranked = TagExtractorFromAnnotator.bySimilarity(null).rank(localFiltered)
                print(baseDir+prefix+".c"+confidence+"s"+support, ranked, id)
            }
        }

        for (score <- EvalParams.contextualScoreInterval) { //TODO make it contextual score by prior
            val confidenceFilter = new ContextualScoreFilter(score)
            for(support <- EvalParams.supportInterval) {
                val supportFilter = new SupportFilter(support)

                var localFiltered = supportFilter.filterOccs(filteredOccList)
                localFiltered = confidenceFilter.filterOccs(localFiltered)

                val ranked = TagExtractorFromAnnotator.byContext(null).rank(localFiltered)
                print(baseDir+prefix+".s"+score+"p"+support, ranked, id)
            }
        }
    }

    def print(fileName: String, ranked: Seq[(DBpediaResource,Double)], id: String) = {
        printSet(fileName+".set",ranked,id)
        printTsv(fileName+".tsv",ranked,id)
    }

    def printSet(fileName: String, ranked: Seq[(DBpediaResource,Double)], id: String) = {
        val out = new PrintStream(new FileOutputStream(fileName, true))
        out.append("\n"+ranked.map(_._1.uri).mkString("\n")+"\n")
        out.close();
    }

    def printTsv(name: String, ranked: Seq[(DBpediaResource,Double)], id: String) = {
        val out = new PrintStream(new FileOutputStream(name, true))
        out.append("\n"+ranked.map{ case (resource,score) => (id,resource.uri,score).productIterator.toList.mkString("\t") }.mkString("\n")+"\n")
        out.close();
    }

    def writeAsEntitySetToFile(filteredOccList: java.util.List[DBpediaResourceOccurrence], file: File) {
        val out = new PrintStream(file)
        val entitySet = filteredOccList
        out.append("\n"+entitySet.map(occ => occ.resource.uri).mkString("\n")+"\n")
        out.close();
    }

    def writeAsEntitySetToFile(occList: List[Tuple2[DBpediaResource,Double]], file: File) {
        val out = new PrintStream(file)
        out.append("\n"+occList.map( t => t._1.uri).mkString("\n")+"\n")
        out.close();
    }

    def now() = {
        new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date())
    }

}