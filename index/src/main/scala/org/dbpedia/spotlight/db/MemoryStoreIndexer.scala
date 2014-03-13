package org.dbpedia.spotlight.db

import memory._
import model.StringTokenizer
import org.apache.commons.lang.NotImplementedException
import java.lang.String


import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import collection.mutable.ListBuffer
import java.util.{Map, Set}
import java.io.File
import org.dbpedia.spotlight.model._
import scala.{Array, Int}
import scala.collection.mutable
import org.dbpedia.spotlight.db.memory.util.StringToIDMapFactory
import scala.Predef._
import scala.Some

/**
 * Implements memory-based indexing. The memory stores are serialized and deserialized using Kryo.
 *
 * @author Joachim Daiber
 */

class MemoryStoreIndexer(val baseDir: File, val quantizedCountStore: MemoryQuantizedCountStore)
  extends SurfaceFormIndexer
  with ResourceIndexer
  with CandidateIndexer
  with TokenTypeIndexer
  with TokenOccurrenceIndexer
{

  //SURFACE FORMS

  def addSurfaceForm(sf: SurfaceForm, annotatedCount: Int, totalCount: Int) {
    throw new NotImplementedException()
  }

  def ngram(sf: String, tokenizer: StringTokenizer): Seq[String] = {
    tokenizer.tokenize(sf)
  }

  def getAllNgrams(grams: Seq[String]): Seq[Seq[String]] = {
    (1 to grams.size-1).flatMap( grams.sliding(_) )
  }

  var tokenizer: Option[StringTokenizer] = None

  def addSurfaceForms(sfCount: Map[SurfaceForm, (Int, Int)], lowercaseCounts: Map[String, Int], MIN_SF_COUNT: Int) {

    val sfStore = new MemorySurfaceFormStore()

    val annotatedCountForID = new Array[Int](sfCount.size + 1)
    val totalCountForID = new Array[Int](sfCount.size + 1)
    val stringForID = new Array[String](sfCount.size + 1)

    val lowercaseMap: mutable.Map[String, mutable.Set[Int]] = new mutable.HashMap[String, mutable.Set[Int]]().withDefaultValue(new mutable.HashSet[Int]())


    var i = 1
    sfCount foreach {
      case (sf, counts) if counts._1 >= MIN_SF_COUNT => {
        stringForID(i) = sf.name
        annotatedCountForID(i) = counts._1
        totalCountForID(i) = counts._2

        val lowerOcc = sfCount.get(new SurfaceForm(sf.name.toLowerCase))
        if ( lowerOcc == null || lowerOcc._1 <= MIN_SF_COUNT ) {
          lowercaseMap.put(sf.name.toLowerCase, lowercaseMap.get(sf.name.toLowerCase) match {
            case Some(s) => s + i
            case None => mutable.HashSet[Int](i)
          })
        }

        i += 1
      }
      case _ =>
    }


    if (tokenizer.isDefined) {

     println("Tokenizing sfs and correcting ngrams")

      //Get all sfs as ngrams in increasing order by their length in tokens
      val sfId = mutable.HashMap[String, Int]()

      //Here be dragons:
      // Correct the counts for sf that are parts of large surface forms:
      // Careful:
      //  We are making an assumption that is not necessarily true:
      //   Assumption: In a Wiki article, an annotation is always of the longest possible surface form.
      //   Example: My [Apple Macbook Pro]. [Apple Macbook Pro] and not [Apple Macbook] Pro
      //
      // Walk the ngrams of the surface forms in increasing order of length from 2 to n, always take the sub-ngrams of size i-1
      // For every sub-ngram: if it is a surface form, reduce its total count by the count of the current surface form.
      stringForID.zipWithIndex.flatMap{
        case (sf: String, id: Int) => {
          val sfNgram = ngram(sf, tokenizer.get)

          sfId.put(sfNgram.mkString(" "),
            sfId.get(sfNgram.mkString(" ")) match {
              case None => id
              case Some(existingID) => if(annotatedCountForID(id) > annotatedCountForID(existingID)) id else existingID
            }
          )

          Some(sfNgram, id)
        }
        case _ => None
      }.sortBy(_._1.size).reverse.foreach{
        case (ngram: Seq[String], id: Int) if(ngram.size > 1) => {
          getAllNgrams(ngram).foreach{ subngram: Seq[String] =>
            sfId.get(subngram.mkString(" ")) match {
              case Some(subID) if(totalCountForID(subID) > 0 && totalCountForID(id) > 0) => totalCountForID(subID) = (totalCountForID(subID) - (1.25 * annotatedCountForID(id))).toInt
              case _ =>
            }
          }
        }
        case _ =>
      }

    }

    //Add lowercased counts:
    println("Adding lowercase map...")
    sfStore.lowercaseMap = new java.util.HashMap[String, Array[Int]]()
    var howmany = 0
    lowercaseCounts.keySet().filter( s => lowercaseMap.contains(s)).foreach { s: String =>
      howmany += 1
      sfStore.lowercaseMap.put(s, (lowercaseCounts.get(s) +: lowercaseMap.get(s).get.toArray))

      if (howmany % 10000 == 0)
        println("Added "+howmany+" lowercase SF")
    }

    sfStore.stringForID  = stringForID
    sfStore.annotatedCountForID = annotatedCountForID.map(quantizedCountStore.addCount)
    sfStore.totalCountForID = totalCountForID.map(quantizedCountStore.addCount)

    MemoryStore.dump(sfStore, new File(baseDir, "sf.mem"))
  }





  //RESOURCES

  def addResource(resource: DBpediaResource, count: Int) {
    throw new NotImplementedException()
  }

  def addResources(resourceCount: Map[DBpediaResource, Int]) {
    val resStore = new MemoryResourceStore()

    val ontologyTypeStore = MemoryStoreIndexer.createOntologyTypeStore(
      resourceCount.keys.flatMap(_.getTypes).toSet.asJava
    )

    val supportForID = new Array[Int](resourceCount.size+1)
    val uriForID = new Array[String](resourceCount.size+1)
    val typesForID = new Array[Array[java.lang.Short]](resourceCount.size+1)

    resourceCount.foreach {

      // (res, count)
      (el: (DBpediaResource, Int)) => {

        supportForID(el._1.id) = el._1.support
        uriForID(el._1.id) = el._1.uri
        typesForID(el._1.id) = (el._1.getTypes map {
          ot: OntologyType => ontologyTypeStore.getOntologyTypeByName(ot.typeID).id}
          ).toArray

      }
    }

    resStore.ontologyTypeStore = ontologyTypeStore
    resStore.supportForID = supportForID.map(quantizedCountStore.addCount).array
    resStore.uriForID = uriForID.array
    resStore.typesForID = typesForID.array

    MemoryStore.dump(resStore, new File(baseDir, "res.mem"))
  }




  def addCandidate(cand: Candidate, count: Int) {
    throw new NotImplementedException()
  }

  def addCandidates(cands: Map[Candidate, Int], numberOfSurfaceForms: Int) {
    val candmapStore = new MemoryCandidateMapStore()

    val candidates      = new Array[ListBuffer[Int]](numberOfSurfaceForms)
    val candidateCounts = new Array[ListBuffer[Int]](numberOfSurfaceForms)

    cands.foreach {
      p: (Candidate, Int) => {
        if(candidates(p._1.surfaceForm.id) == null) {
          candidates(p._1.surfaceForm.id)      = ListBuffer[Int]()
          candidateCounts(p._1.surfaceForm.id) = ListBuffer[Int]()
        }

        candidates(p._1.surfaceForm.id)      += p._1.resource.id
        candidateCounts(p._1.surfaceForm.id) += p._2
      }
    }

    candmapStore.candidates = (candidates map { l: ListBuffer[Int] => if(l != null) l.toArray else null} ).toArray
    candmapStore.candidateCounts = (candidateCounts map { l: ListBuffer[Int] => if(l != null) l.map(quantizedCountStore.addCount).toArray else null} ).toArray

    MemoryStore.dump(candmapStore, new File(baseDir, "candmap.mem"))
  }

  def addCandidatesByID(cands: Map[Pair[Int, Int], Int], numberOfSurfaceForms: Int) {
    val candmapStore = new MemoryCandidateMapStore()

    val candidates      = new Array[Array[Int]](numberOfSurfaceForms)
    val candidateCounts = new Array[Array[Int]](numberOfSurfaceForms)

    cands.foreach {
      p: (Pair[Int, Int], Int) => {

        if(candidates(p._1._1) == null) {
          candidates(p._1._1)      = Array[Int]()
          candidateCounts(p._1._1) = Array[Int]()
        }

        candidates(p._1._1)      :+= p._1._2
        candidateCounts(p._1._1) :+= p._2
      }
    }

    candmapStore.candidates = candidates
    candmapStore.candidateCounts = candidateCounts.map(cs =>
      if (cs == null)
        null
      else
        cs.map(quantizedCountStore.addCount).array
    )

    MemoryStore.dump(candmapStore, new File(baseDir, "candmap.mem"))
  }

  def addTokenType(token: TokenType, count: Int) {
    throw new NotImplementedException()
  }

  def addTokenTypes(tokenCount: Map[TokenType, Int]) {

    val tokenTypeStore = new MemoryTokenTypeStore()

    val tokens = new Array[String](tokenCount.size)
    val counts = new Array[Int](tokenCount.size)

    tokenCount.foreach {
      case (token, count) => {
        tokens(token.id) = token.tokenType
        counts(token.id) = count
      }
    }

    tokenTypeStore.tokenForId = tokens.array
    tokenTypeStore.counts = counts.array

    MemoryStore.dump(tokenTypeStore, new File(baseDir, "tokens.mem"))
  }


  //TOKEN OCCURRENCES

  def addTokenOccurrence(resource: DBpediaResource, token: TokenType, count: Int) {
    throw new NotImplementedException()
  }

  def addTokenOccurrence(resource: DBpediaResource, tokenCounts: Map[Int, Int]) {
    throw new NotImplementedException()
  }

  lazy val contextStore = new MemoryContextStore()

  def createContextStore(n: Int) {
    contextStore.tokens = new Array[Array[Int]](n)
    contextStore.counts = new Array[Array[Short]](n)
  }

  def addTokenOccurrences(occs: Map[DBpediaResource, Map[Int, Int]]) {
    occs.foreach{ case(res, tokenCounts) => {
      val (t, c) = tokenCounts.unzip
      contextStore.tokens(res.id) = t.toArray
      contextStore.counts(res.id) = c.map(quantizedCountStore.addCount).toArray
    }
    }
  }

  def addTokenOccurrences(occs: Iterator[Triple[DBpediaResource, Array[TokenType], Array[Int]]]) {
    occs.filter(t => t!=null && t._1 != null).foreach{
      t: Triple[DBpediaResource, Array[TokenType], Array[Int]] => {
        val Triple(res, tokens, counts) = t
        if (res != null) {
          assert (tokens.size == counts.size)
          if(contextStore.tokens(res.id) != null) {
            val (mergedTokens, mergedCounts) = (tokens.map{ t: TokenType => t.id }.array.zip(counts.array) ++ contextStore.tokens(res.id).zip( contextStore.counts(res.id).map(quantizedCountStore.getCount) )).groupBy(_._1).map{ case(k, v) => (k, v.map{ p => p._2}.sum ) }.unzip
            contextStore.tokens(res.id) = mergedTokens.toArray.array
            contextStore.counts(res.id) = mergedCounts.asInstanceOf[Iterable[Int]].map(quantizedCountStore.addCount).toArray.array
          } else{
            contextStore.tokens(res.id) = tokens.map{ t: TokenType => t.id }.array
            contextStore.counts(res.id) = counts.map(quantizedCountStore.addCount).array
          }
        }
      }
    }
  }


  /**
   * Iterates the Context Store sorting the tokens by their token Id.
   */
  def sortTokensInContextStore(){
    for((currentTokens, i) <- contextStore.tokens.zipWithIndex){
      if (currentTokens.isInstanceOf[Array[Int]] && currentTokens.size > 1){
        val (sortedTokens, counts) = currentTokens.zip(contextStore.counts(i)).sortBy(_._1).unzip
        contextStore.tokens(i) = sortedTokens.toArray
        contextStore.counts(i) = counts.toArray
      }
    }
  }

  def writeTokenOccurrences() {
    sortTokensInContextStore()
    MemoryStore.dump(contextStore, new File(baseDir, "context.mem"))
  }

  def writeQuantizedCounts() {
    MemoryStore.dump(quantizedCountStore, new File(baseDir, "quantized_counts.mem"))
  }



}

object MemoryStoreIndexer {

  def createOntologyTypeStore(types: Set[OntologyType]): MemoryOntologyTypeStore = {
    val idFromName = new java.util.HashMap[String, java.lang.Short]()
    val ontologyTypeFromID = new java.util.HashMap[java.lang.Short, OntologyType]()

    var i = 0.toShort
    types foreach {
      ontologyType: OntologyType =>
        ontologyType.id = i

        ontologyTypeFromID.put(ontologyType.id, ontologyType)
        idFromName.put(ontologyType.typeID, ontologyType.id)

        i = (i + 1).toShort
    }

    val otStore = new MemoryOntologyTypeStore()
    otStore.idFromName = idFromName
    otStore.ontologyTypeFromID = ontologyTypeFromID

    otStore
  }

}
