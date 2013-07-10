package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, SpotlightFactory, SpotlightConfiguration}
import org.dbpedia.spotlight.disambiguate.TwoStepDisambiguator
import org.dbpedia.spotlight.corpus.{PredoseCorpus}
import java.io.{PrintWriter, File}
import org.dbpedia.spotlight.filter.occurrences.{RedirectResolveFilter, UriWhitelistFilter, OccurrenceFilter}
import org.dbpedia.spotlight.io.TSVOutputGenerator

/**
 *
 * @author pablomendes
 */

class EvaluatePredose {

    def main(args : Array[String]) {

        val config = new SpotlightConfiguration("conf/predose.properties");
        val factory = new SpotlightFactory(config)

        //val topics = HashMapTopicalPriorStore.fromDir(new File("data/topics"))
        val disambiguators = Set(//new TopicalDisambiguator(factory.candidateSearcher,topics),
                                 //new TopicalFilteredDisambiguator(factory.candidateSearcher,factory.contextSearcher,topics)
                                 new TwoStepDisambiguator(factory.candidateSearcher,factory.contextSearcher)
                                 //, new CuttingEdgeDisambiguator(factory),
                                 //new PageRankDisambiguator(factory)
                                )

        val sources = List(PredoseCorpus.fromFile(new File("/home/pablo/eval/predose/predose_annotations.tsv")))


        val onlyDBpedia = new OccurrenceFilter {
            def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
                if (occ.resource.uri.startsWith("http://knoesis")) {
                    None
                } else {
                    Some(occ)
                }
            }
        }

        val redirectTCFileName  = if (args.size>1) args(1) else "data/redirects_tc.tsv" //produced by ExtractCandidateMap
        val conceptURIsFileName  = if (args.size>2) args(2) else "data/conceptURIs.list" //produced by ExtractCandidateMap
        //UriWhitelistFilter.fromFile(new File(conceptURIsFileName))
        val occFilters = List(RedirectResolveFilter.fromFile(new File(redirectTCFileName)),
                              onlyDBpedia)

        sources.foreach( paragraphs => {
          val testSourceName = paragraphs.name
          disambiguators.foreach( d => {
              val dName = d.name.replaceAll("""[.*[?/<>|*:\"{\\}].*]""","_")
              val tsvOut = new TSVOutputGenerator(new PrintWriter("%s-%s-%s.pareval.log".format(testSourceName,dName,EvalUtils.now())))
              //val arffOut = new TrainingDataOutputGenerator()
              val outputs = List(tsvOut)
              EvaluateParagraphDisambiguator.evaluate(paragraphs, d, outputs, occFilters)
              outputs.foreach(_.close)
          })
        })
    }


}
