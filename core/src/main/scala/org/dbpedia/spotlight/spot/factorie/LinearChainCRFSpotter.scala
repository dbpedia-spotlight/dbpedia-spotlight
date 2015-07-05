package org.dbpedia.spotlight.spot.factorie

import org.dbpedia.spotlight.model._
import cc.factorie._
import cc.factorie.model._
import cc.factorie.optimize._
import cc.factorie.variable._
import org.dbpedia.spotlight.db.model.{TextTokenizer, ResourceStore}
import org.dbpedia.spotlight.io.AnnotatedTextSource
import akka.actor.{ActorSystem, Props, Actor}
import scala.util.control.Breaks._
import akka.routing.{SmallestMailboxPool, SmallestMailboxRouter}
import org.dbpedia.spotlight.spot.Spotter
import java.util
import akka.pattern._
import java.util.concurrent.TimeUnit
import org.apache.commons.logging.LogFactory
import java.io.File
import org.dbpedia.spotlight.model.Factory.OntologyType
import org.dbpedia.spotlight.model.OntologyType
import scala.Predef._
import org.dbpedia.spotlight.log.SpotlightLog
import cc.factorie.util.BinarySerializer
import cc.factorie.la.{SynchronizedDoubleAccumulator, SynchronizedWeightsMapAccumulator}
import cc.factorie.infer.{InferByBPChain, BP}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Training of this Spotter via LinearChainCRFSpotter.fromAnnotatedTextSource(...)
 */
class LinearChainCRFSpotter(types:Set[OntologyType] = Set[OntologyType](), tokenizer:TextTokenizer = null) extends Spotter {

  // The variable classes
  object TokenDomain extends CategoricalVectorDomain[String]
  class Token(val t:org.dbpedia.spotlight.model.Token, labelString:String="O") extends BinaryFeatureVectorVariable[String] with ChainLink[Token,Sentence] {
    def domain = TokenDomain
    val label: Label = new Label(labelString, this)

    { //Add features
      val term = { if(t.tokenType == TokenType.STOPWORD) t.token else t.tokenType.tokenType }
      this += "W="+term
      //if (term.length > 3) this += "PRE="+term.substring(0,3)
      if (t.token.head.isUpper) this += "CAPITALIZED"
      if (t.token.matches("^[0-9]+$")) this += "NUMERIC"
      if (t.token.matches("[-,\\.;:?!()]+")) this += "PUNCTUATION"
      if (t.tokenType == TokenType.STOPWORD) this += "STOPWORD"
    }
  }

  object LabelDomain extends CategoricalDomain[String]({
      Set("B-","I-","L-","U-").map(_ +"SPOT") ++ Set("O") ++ types.flatMap(t => Set("B-","I-","L-","U-").map(_ + t.getFullUri))
  })
  LabelDomain.freeze()
  class Label(labelname: String, val token: Token) extends LabeledCategoricalVariable(labelname) {
    def domain = LabelDomain
    def hasNext = token.hasNext && token.next.label != null
    def hasPrev = token.hasPrev && token.prev.label != null
    def next = token.next.label
    def prev = token.prev.label
  }

  class Sentence extends Chain[Sentence,Token] {
    override def +=(t: Token) = {
      super.+=(t)
      if(t.hasPrev) {
        t ++= t.prev.activeCategories.filter(!_.contains('@')).map(_+"@-1")
        t.prev ++= t.activeCategories.filter(!_.contains('@')).map(_+"@+1")
        if(t.prev.hasPrev) {
          t ++= t.prev(2).activeCategories.filter(!_.contains('@')).map(_+"@-2")
        }
      }
      this
    }
  }

  // The model
  val nerModel = new TemplateModel with Parameters {
    // Bias term on each individual label
    object bias extends DotTemplateWithStatistics1[Label] {
      val weights = Weights(new la.DenseTensor1(LabelDomain.size))
    }
    // Transition factors between two successive labels
    object transtion extends DotTemplateWithStatistics2[Label, Label] {
      val weights = Weights(new la.DenseTensor2(LabelDomain.size, LabelDomain.size))
      def unroll1(label: Label) = if (label.hasPrev) Factor(label.prev, label) else Nil
      def unroll2(label: Label) = if (label.hasNext) Factor(label, label.next) else Nil
    }
    // Factor between label and observed token
    object evidence extends DotTemplateWithStatistics2[Label, Token] {
      val weights = Weights(new la.GrowableDenseTensor2(LabelDomain.size, TokenDomain.dimensionSize))
      def unroll1(label: Label) = Factor(label, label.token)
      def unroll2(token: Token) = throw new Error("Token values shouldn't change")
    }
    this += evidence
    this += bias
    this += transtion
  }

  def extract(text: Text) = {
    val sfOccs = new util.ArrayList[SurfaceFormOccurrence]()

    if(tokenizer!=null)
      tokenizer.tokenizeMaybe(text)
    try {
      val tokens = text.features("tokens").value.asInstanceOf[List[org.dbpedia.spotlight.model.Token]]

      val sentence = new Sentence
      val labels = tokens.map(t => {
        val token = new Token(t)
        sentence += token
        token.label
      })

      InferByBPChain.infer(labels, nerModel).setToMaximize(null)

      //start & end of current spot & type
      def addOccurence(acc: (Int, Int, String)) {
        val sfOcc = new SurfaceFormOccurrence(new SurfaceForm(text.text.substring(acc._1, acc._2)), text, acc._1)
        if(acc._3 != "SPOT")
          sfOcc.setFeature(new Feature("ontology-type", acc._3))
        sfOccs.add(sfOcc)
      }

      labels.foldLeft((-1,-1,""))((acc,label) => {
        val typ = ontologyTypeFromLabel(label.categoryValue)
        if(acc._1 < 0) {
          if(typ.isEmpty) acc
          else (getStart(label), getEnd(label), typ)
        } else {
          if(typ.isEmpty){
            addOccurence(acc)
            (-1,-1,"")
          } else {
            if(typ == acc._3)
              (acc._1,getEnd(label), typ)
            else {
              addOccurence(acc)
              (getStart(label), getEnd(label), typ)
            }
          }
        }
      })

      sfOccs
    }
    catch {
      case e:NoSuchElementException => SpotlightLog.error(getClass,"Text was not tokenized! No Spotting possible!"); null
    }
  }

  private def ontologyTypeFromLabel(l:String) = if(l=="O") "" else l.substring(2)

  private def getEnd(label: Label): Int = {
    label.token.t.offset + label.token.t.token.length
  }

  private def getStart(label: Label): Int = {
    label.token.t.offset
  }

  private var name = "Linear-Chain-NER-Spotter"
  def getName = name
  def setName(name: String) = this.name = name

}

object LinearChainCRFSpotter {

  private final val LOG = LogFactory.getLog(getClass)


  /**
   * @param source Source from which the model is trained
   * @param types Ontology types that are trained on. If empty only special type "SPOT" is used
   * @param resStore ResourceStore for ontology type lookup, if null, it is assumed, that resources already have types, or no types at all are considered, except for "SPOT"
   * @param sortOccurrences set to true if occurences do not appear in ordered by textOffset
   * @param batchSize size of batch for mini batch training; <0 means batch training
   * @param maxIterations
   * @param tokenizer needed if texts are not already tokenized
   * @return LinearChainCRFSpotter
   */
  def fromAnnotatedTextSource(source:AnnotatedTextSource,
                              types:Set[OntologyType] = Set[OntologyType](),
                              resStore:ResourceStore=null,
                              sortOccurrences:Boolean = false,
                              batchSize:Int = -1,
                              maxIterations:Int = Int.MaxValue,
                              tokenizer:TextTokenizer = null) = {

    val model = new LinearChainCRFSpotter(if(resStore == null) Set[OntologyType]() else types, tokenizer)
    val typesSeq = types.toSeq
    val trainer = new Trainer(model,batchSize)

    var iteration = 0

    while(!trainer.isConverged && iteration < maxIterations) {
      iteration += 1

      breakable { source.foreach(paragraph => {
        if(tokenizer != null)
          tokenizer.tokenizeMaybe(paragraph.text)

        val tokens = paragraph.text.features("tokens").value.asInstanceOf[List[org.dbpedia.spotlight.model.Token]]
        val occsIt = { if(sortOccurrences) paragraph.occurrences.sortBy(_.textOffset).iterator else paragraph.occurrences.iterator }

        if(occsIt.hasNext) {
          var currentOcc = occsIt.next()
          val sentence = new model.Sentence

          val labels = tokens.map(t => {
            val start = t.offset
            val end = start + t.token.size

            val token:model.Token = {
              if(currentOcc != null && currentOcc.textOffset <= start && currentOcc.textOffset + currentOcc.surfaceForm.name.size >= end) {
                val occStart = currentOcc.textOffset
                val occEnd = occStart + currentOcc.surfaceForm.name.size

                val ts = {
                  if(resStore == null) currentOcc.resource.types.intersect(typesSeq)
                  else {
                    try {
                      resStore.getResourceByName(currentOcc.resource.uri).types.intersect(typesSeq)
                    }
                    catch {
                      case t:Throwable => LOG.debug("Could not find uri: "+currentOcc.resource.uri); Seq[OntologyType]()
                    }
                  }
                }
                val typ = {
                  if(!ts.isEmpty)
                    ts.find(_.getFullUri.startsWith(DBpediaType.DBPEDIA_ONTOLOGY_PREFIX)).getOrElse(ts.head).getFullUri
                  else "SPOT"
                }

                //Use BILOU
                if(!typ.isEmpty)
                  if(occStart == start) {
                    if(occEnd == end) {
                      if(occsIt.hasNext) currentOcc = occsIt.next()
                      else currentOcc = null
                      new model.Token(t,"U-" + typ)
                    }
                    else new model.Token(t,"B-" + typ)
                  }
                  else if(occEnd == end) {
                    if(occsIt.hasNext) currentOcc = occsIt.next()
                    else currentOcc = null
                    new model.Token(t,"L-" + typ)
                  }
                  else new model.Token(t, "I-" + typ)
                else
                  new model.Token(t)
              } else {
                while(currentOcc != null && currentOcc.textOffset + currentOcc.surfaceForm.name.size <= start){
                  if(occsIt.hasNext) currentOcc = occsIt.next()
                  else currentOcc = null
                }

                new model.Token(t)
              }
            }
            sentence += token
            token.label
          })

          trainer.processExample(labels.asInstanceOf[Seq[trainer.model.Label]])

          //Ugly
          if(trainer.isConverged)
            break()
        }
      }) }
      if(!trainer.isConverged && batchSize < 0)
        trainer.step
    }

    model
  }

  private class ExampleProcessingActor(gradientAccumulator: SynchronizedWeightsMapAccumulator,
                                         valueAccumulator: SynchronizedDoubleAccumulator) extends Actor {
    def receive = {
      case example:Example =>
        example.accumulateValueAndGradient(valueAccumulator,gradientAccumulator)
        sender ! DONE
    }
  }
  private object DONE

  private class Trainer(val model:LinearChainCRFSpotter, batchSize:Int, optimizer:GradientOptimizer= new LBFGS with L2Regularization) {
    val gradientAccumulator = new SynchronizedWeightsMapAccumulator(model.nerModel.parameters.blankDenseMap)
    val valueAccumulator = new SynchronizedDoubleAccumulator()

    var localCounter = 0

    val context = ActorSystem("Ner-Training")
    val processingActors = context.actorOf(
      SmallestMailboxPool(nrOfInstances = Runtime.getRuntime().availableProcessors()).props(Props(classOf[ExampleProcessingActor],gradientAccumulator,valueAccumulator)))
    //var responses:List[Awaitable[Any]] = List[Awaitable[Any]]()

    var examples =List[Example]()

    //Cannot process examples directly here, because TokenDomain is growable, so name of method is a little misleading
    def processExample(labels:Seq[model.Label]) {
      examples ::= new LikelihoodExample(labels,model.nerModel,InferByBPChain)
      if(batchSize > 0) {
        localCounter += 1
        if(localCounter % batchSize == 0) {
          //Update the model
          step
        }
      }
    }

    def step = {
      val responses = examples.map(example => processingActors.ask(example)(akka.util.Timeout(600, TimeUnit.SECONDS)))
      responses.foreach(resp => Await.result(resp, Duration.apply(600, TimeUnit.SECONDS)))
      examples = List[Example]()

      optimizer.step(model.nerModel.parameters, gradientAccumulator.tensorSet, valueAccumulator.l.value)
      LOG.info(TrainerHelpers.getBatchTrainerStatus(gradientAccumulator.l.tensorSet.oneNorm, valueAccumulator.l.value, 0L))

      gradientAccumulator.tensorSet.zero()
      valueAccumulator.accumulate(-valueAccumulator.l.value)
    }

    def isConverged = optimizer.isConverged
  }

  def main(args:Array[String]) {
    val model = new LinearChainCRFSpotter(Set(OntologyType.fromURI("http://dbpedia.org/ontology/Person"),
      OntologyType.fromURI("http://dbpedia.org/ontology/Organisation"),
      OntologyType.fromURI("http://dbpedia.org/ontology/Place")))

    serialize(new File("/tmp/model.bin"), model)
    val model2 = deserialize(new File("/tmp/model.bin"))
    model2
  }

  def deserialize(file:File,tokenizer:TextTokenizer = null) = {
    val model = new LinearChainCRFSpotter(tokenizer = tokenizer)
    BinarySerializer.deserialize(
      model.TokenDomain,
      model.LabelDomain,
      model.nerModel,
      file)
    model
  }

  def serialize(file:File, model:LinearChainCRFSpotter) {
    BinarySerializer.serialize(
      model.TokenDomain,
      model.LabelDomain,
      model.nerModel,
      file)
  }

}
