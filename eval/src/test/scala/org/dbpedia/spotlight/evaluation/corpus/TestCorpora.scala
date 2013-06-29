package org.dbpedia.spotlight.evaluation.corpus


import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.dbpedia.spotlight.corpus.{KBPCorpus, AidaCorpus, MilneWittenCorpus}
import java.io.File

/**
 *
 * @author pablomendes
 */

class TestCorpora extends FlatSpec with ShouldMatchers {
  implicit def stringToFile(s: String) = new File(s)

  val queryFile = new File("/mnt/windows/Extra/Researches/data/kbp/queries/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.0/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.1/data/tac_2010_kbp_evaluation_entity_linking_queries.xml")
  val annotationFile = new File("/mnt/windows/Extra/Researches/data/kbp/queries/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.0/TAC_2010_KBP_Evaluation_Entity_Linking_Gold_Standard_V1.1/data/tac_2010_kbp_evaluation_entity_linking_query_types.tab")
  val sourceDir = new File("/mnt/windows/Extra/Researches/data/kbp/kbp2011/TAC_KBP_2010_Source_Data/TAC_2010_KBP_Source_Data/data")
  val kbDir = new File("/mnt/windows/Extra/Researches/data/kbp/kbp2011/TAC_2009_KBP_Evaluation_Reference_Knowledge_Base/data")


  val corpora = List(MilneWittenCorpus.fromDirectory("/home/pablo/eval/wikify/original/"),
    AidaCorpus.fromFile("/home/pablo/eval/aida/gold/CoNLL-YAGO.tsv"),
    new KBPCorpus(queryFile, annotationFile, sourceDir, kbDir)
  )


  corpora.foreach(corpus => {
    corpus.getClass.getSimpleName.trim should "have correct offsets" in {
      corpus.foreach(p => {
        p.occurrences.foreach(o => {
          val stored = o.surfaceForm.name
          val actual = o.context.text.substring(o.textOffset, o.textOffset + stored.length)
          stored should equal(actual)
        })
      })
    }
  })


}
