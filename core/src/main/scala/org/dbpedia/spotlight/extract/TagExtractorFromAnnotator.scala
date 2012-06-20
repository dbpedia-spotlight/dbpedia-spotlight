package org.dbpedia.spotlight.extract

import org.dbpedia.spotlight.model.{OntologyType, Text, DBpediaResourceOccurrence}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.annotate.{ParagraphAnnotator, Annotator}

/**
 * Wraps around an Annotator object to provide TagExtractor functionality.
 * Needs to know how to extract a ranked list of (DBpediaResource,Double) from DBpediaResourceOccurrences
 * Attention: will only extract tags that were explicitly mentioned by name.
 * If Berlin, the Allies and 1945 were mentioned, it is clear that the WWWII is a relevant concept.
 * However, if no surface form of WWWII is explicitly mentioned in the text, it will not be extracted by this method.
 * This is a limitation of the annotation -> extraction approach.
 * Use native extractors (e.g. LuceneTagExtractor) if you would like all kinds of related concepts.
 *
 * @author pablomendes
 */
//TODO make it work for Annotator besides ParagraphAnnotator. Will need some interface rework between those two.
class TagExtractorFromAnnotator(val annotator: ParagraphAnnotator, val getValue: DBpediaResourceOccurrence => Double) extends TagExtractor {

    override def extract(text: Text, nTags: Int) = {
        val occs = annotator.annotate(text.text)
        rank(occs)
    }

    override def extract(text: Text, nTags: Int, ontologyTypes: List[OntologyType]) = {
        extract(text,nTags).filter(_._1.types.intersect(ontologyTypes).size > 0) // the resource has a type in the ontologyTypesList
    }

    def rank(occs: Traversable[DBpediaResourceOccurrence]) = {
        // get tuples from uri to one of the scores
        val tuples = occs.map(occ => (occ.resource, getValue(occ)))
        // group by uri, sum on value, sort by value (desc)
        tuples.groupBy(_._1).map { case (k,list) => {
            val sum = list.foldLeft(0.0) { (acc,b) => acc + b._2 }
            (k,sum)
        }}.toList
            .sortBy(_._2)
            .reverse
    }

}

/**
 * Factory object for tag extractors that use annotators as internal implementation
 */
object TagExtractorFromAnnotator {

    /**
     * creates a tag extractor that uses the similarity score from the annotation as ranking score
     */
    def bySimilarity(annotator: ParagraphAnnotator) = {
        new TagExtractorFromAnnotator(annotator, _.similarityScore)
    }

    /**
     * creates a tag extractor that uses the prior score from the annotation as ranking score
     */
    def byPrior(annotator: ParagraphAnnotator) = {
        new TagExtractorFromAnnotator(annotator, _.resource.prior)
    }

    /**
     * creates a tag extractor that uses the confusion score from the annotation as ranking score
     */
    def byConfusion(annotator: ParagraphAnnotator) = {
        new TagExtractorFromAnnotator(annotator, occ => occ.percentageOfSecondRank * -1) // small percentage is large gap.
    }

    /**
     * creates a tag extractor that uses the confidence score from the annotation as ranking score
     */
    def byConfidence(annotator: ParagraphAnnotator) = {
        new TagExtractorFromAnnotator(annotator, occ => (occ.similarityScore * (1-occ.percentageOfSecondRank)))
    }

    /**
     * creates a tag extractor that uses the contextual score from the annotation as ranking score
     */
    def byContext(annotator: ParagraphAnnotator) = {
        new TagExtractorFromAnnotator(annotator, _.contextualScore)
    }
}