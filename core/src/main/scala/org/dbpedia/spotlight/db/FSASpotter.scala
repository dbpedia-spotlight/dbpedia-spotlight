package org.dbpedia.spotlight.db

import memory.MemoryStore
import org.dbpedia.spotlight.model._
import model.{TextTokenizer, StringTokenizer, SurfaceFormStore}
import opennlp.tools.util.Span
import scala.collection.mutable.{ListBuffer, ArrayBuffer, Map}
import tokenize.LanguageIndependentTokenizer
import util.control.Breaks._


/**
 * @author Joachim Daiber
 */

class FSASpotter(
  fsaDictionary: FSADictionary,
  surfaceFormStore: SurfaceFormStore,
  spotFeatureWeights: Option[Seq[Double]],
  stopwords: Set[String]
) extends DBSpotter(surfaceFormStore, spotFeatureWeights, stopwords) {

  def generateCandidates(sentence: List[Token]): Seq[Span] = {
    val initialSpans = findUppercaseSequences(sentence.map(_.token).toArray)
    FSASpotter.generateCandidates(sentence, fsaDictionary, initialSpans = initialSpans)
  }

  def typeOrder = Array("Capital_Sequences", "m")

  private var name = "FSA dictionary spotter"
  def getName = name
  def setName(name: String) {
    this.name = name
  }

}

class AllOccurrencesFSASpotter(
                                fsaDictionary: FSADictionary,
                                tokenizer: TextTokenizer
                                ) {

  def extract(text: String): List[(java.lang.String, Int)] = {

    var spots = ListBuffer[(String, Int)]()
    val sentences: List[List[Token]] = DBSpotter.tokensToSentences(tokenizer.tokenize(new Text(text)))

    //Go through all sentences
    sentences.foreach{ sentence: List[Token] =>
      FSASpotter.generateCandidates(sentence, fsaDictionary).foreach(chunkSpan => {
        val firstToken = chunkSpan.getStart
        val lastToken = chunkSpan.getEnd-1
        val startOffset = sentence(firstToken).offset
        val endOffset = sentence(lastToken).offset + sentence(lastToken).token.length

        spots += ((text.substring(startOffset, endOffset), startOffset))
      })
    }

    spots.toList
  }
}

object FSASpotter {

  //The initial state
  val INITIAL_STATE = 0

  //State ID for the accepting state. Note that in sorting, we rely on this being < 0.
  val ACCEPTING_STATE = -1

  //State ID for None
  val REJECTING_STATE = -2

  def generateCandidates(sentence: List[Token], fsaDictionary: FSADictionary, initialSpans: Seq[Span] = Seq[Span]()): Seq[Span] = {

    var spans = initialSpans

    val ids = sentence.map(_.tokenType.id)
    sentence.zipWithIndex.foreach {
      case (t: Token, i: Int) => {

        var currentState = FSASpotter.INITIAL_STATE
        var j = i

        do {
          //Get the transition for the next token:
          val (endState, nextState) = fsaDictionary.next(currentState, ids(j))

          //Add a span if this is a possible spot:
          if (endState == FSASpotter.ACCEPTING_STATE)
            spans :+= new Span(i, j+1, "m")

          //Keep traversing the FSA until a rejecting state or the end of the sentence:
          currentState = nextState
          j += 1
        } while ( currentState != FSASpotter.REJECTING_STATE && j < sentence.length )
      }
    }

    spans
  }


  def buildDictionary(sfStore: SurfaceFormStore, tokenizer: TextTokenizer): FSADictionary = {
    buildDictionaryFromIterable(sfStore.iterateSurfaceForms.filter(_.annotationProbability >= 0.05).map(_.name), tokenizer)
  }

  def buildDictionaryFromIterable(iterable: Iterable[String], tokenizer: TextTokenizer): FSADictionary = {

    //Temporary FSA DSs:
    val transitions: ArrayBuffer[Map[Int, Int]] = ArrayBuffer[Map[Int, Int]]()
    val transitionsToX: ArrayBuffer[Set[Int]] = ArrayBuffer[Set[Int]]()
    transitions.append( Map[Int, Int]() )
    transitionsToX.append(Set[Int]())

    //Get the next state given token during training:
    def nextTrain(state: Int, token: Int): Option[Int] = transitions(state).get(token)

    //Add transition to nextState
    def addTransition(state: Int, token: Int, nextState: Int) {
      if (nextState == ACCEPTING_STATE)
        transitionsToX(state) += token
      else
        transitions(state).put(token, nextState)
    }

    //Add transition to a new state
    def addNewTransition(state: Int, token: Int): Int = {
      if (nextTrain(state, token).isDefined) {
        nextTrain(state, token).get
      } else {
        transitions.append( Map[Int, Int]() )
        transitionsToX.append(Set[Int]())

        addTransition(state, token, transitions.size-1)
        transitions.size-1
      }
    }

    var z = 0

    System.err.println("Tokenizing SFs...")
    iterable.grouped(100000).toList.par.flatMap(_.map{
      sfString: String =>
        //Tokenize all SFs first
        tokenizer.tokenize(new Text(sfString))
    }).seq.foreach{
      case tokens: Seq[Token] if tokens.size > 0 =>
        z+=1
        if ((z % 100000) == 0)
          System.err.println("Processed %d SFs.".format(z))

        val ids = tokens.map(_.tokenType.id).toArray

        //For each token in the SF, add the transitions to the FSA:
        var currentState = INITIAL_STATE
        if (ids.size > 1)
          (0 until ids.size-1).foreach { j: Int =>
            currentState = addNewTransition(currentState, ids(j))
          }

        addTransition(currentState, ids.last, ACCEPTING_STATE)
      case _ =>
    }

    val d = new FSADictionary()

    //Convert the temporary transition storage to arrays:
    val pairs = transitions.zip(transitionsToX).map{ case (ts: Map[Int, Int], es: Set[Int]) =>
      (ts.iterator ++ es.map{ t: Int => (t, ACCEPTING_STATE) }).toList.sortBy(p => (p._1, p._2))
    }

    d.transitionsTokens = pairs.map(_.map(_._1).toArray).toArray
    d.transitionsStates = pairs.map(_.map(_._2).toArray).toArray

    d
  }

}

@SerialVersionUID(2001001)
class FSADictionary extends MemoryStore {

  var transitionsTokens: Array[Array[Int]] = null
  var transitionsStates: Array[Array[Int]] = null

  /**
   * Returns the index of the first ocurrance of Token inside the transition tokens
   * of a given state
   *
   * @param state
   * @param token
   * @return index of the first occurance
   */
  def searchTokenInTransitions(state:Int, token:Int):Int = {

    var i = java.util.Arrays.binarySearch(transitionsTokens(state), token)

    /* Checks if previous element has the same transitionToken
      (odd cases in binary Search)
    */
    if (i > 0 && transitionsTokens(state)(i-1) == token){
      i = i -1
    }

    i
  }

  /**
   * Returns state transitions in the form:
   * (accepting state, next state)
   *
   * @param state
   * @param token
   * @return
   */
  def next(state: Int, token: Int): (Int, Int) = {
    val i = searchTokenInTransitions(state, token)

    if(i < 0) {
      (FSASpotter.REJECTING_STATE, FSASpotter.REJECTING_STATE)
    } else {

      if (transitionsTokens(state).length > i+1 && transitionsTokens(state)(i+1) == token)
        (FSASpotter.ACCEPTING_STATE, transitionsStates(state)(i+1))
      else if (transitionsStates(state)(i) != FSASpotter.ACCEPTING_STATE)
        (FSASpotter.REJECTING_STATE, transitionsStates(state)(i))
      else
        (FSASpotter.ACCEPTING_STATE, FSASpotter.REJECTING_STATE)

    }
  }

  def size = transitionsStates.size

}
