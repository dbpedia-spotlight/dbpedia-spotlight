package org.dbpedia.spotlight.evaluation.corpus


import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.dbpedia.spotlight.corpus.{AidaCorpus, MilneWittenCorpus}
import java.io.File

/**
 *
 * @author pablomendes
 */

class TestCorpora extends FlatSpec with ShouldMatchers {
    implicit def stringToFile(s: String) = new File(s)
    val corpora = List(MilneWittenCorpus.fromDirectory("/home/pablo/eval/wikify/original/"),
                         AidaCorpus.fromFile("/home/pablo/eval/aida/gold/CoNLL-YAGO.tsv"))


    corpora.foreach( corpus => {
        corpus.getClass.getSimpleName.trim should "have correct offsets" in {
            corpus.foreach( p => {
                p.occurrences.foreach( o => {
                    val stored = o.surfaceForm.name
                    val actual = o.context.text.substring(o.textOffset,o.textOffset+stored.length)
                    stored should equal (actual)
                })
            })
        }
    })


}
