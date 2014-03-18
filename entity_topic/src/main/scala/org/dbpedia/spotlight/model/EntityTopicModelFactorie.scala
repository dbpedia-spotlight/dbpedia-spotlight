package org.dbpedia.spotlight.model

import cc.factorie.variable._
import cc.factorie.la._
import scala.util.Random
import cc.factorie.directed._

/**
 * Created by dirkw on 3/4/14.
 */
class EntityTopicModel(val numTopics:Int,
                       val entityDomain:CategoricalDomain[String],
                       val mentionDomain:CategoricalDomain[String],
                       val maxNrOfWords:Int) {
    private implicit val random = new Random()
    entityDomain.freeze()
    mentionDomain.freeze()

    object EntitySeqDomain extends CategoricalSeqDomain[String] {
        override lazy val elementDomain = entityDomain
    }
    class Entities(name:String,theta:ProportionsVariable, strings:Seq[String]) extends DiscreteSeqVariable(strings.map(entityDomain.indexOnly)) {
        def domain = EntitySeqDomain
    }

    object TopicDomain extends DiscreteDomain(numTopics)
    object TopicSeqDomain extends DiscreteSeqDomain { override val elementDomain = TopicDomain }
    class Topics(len:Int) extends DiscreteSeqVariable(len) { def domain = TopicSeqDomain }

    object MentionSeqDomain extends CategoricalSeqDomain[String] {
        override lazy val elementDomain = mentionDomain
    }
    class Mentions(val mentions:Seq[String], val entities:Entities) extends CategoricalSeqVariable(mentions) {
        def domain = MentionSeqDomain
    }

    class Assignments(val entities:Entities, val words:Seq[String]) extends DiscreteSeqVariable(words.size) {
        def domain = EntitySeqDomain
    }

    object WordSeqDomain extends CategoricalSeqDomain[String]
    val wordDomain = WordSeqDomain.elementDomain
    wordDomain.maxSize = maxNrOfWords
    wordDomain.growPastMaxSize = false
    class Words(words:Seq[String]) extends CategoricalSeqVariable(words.filter(w => wordDomain.index(w) >= 0)) {
        def domain = WordSeqDomain
    }

    case class Document(name:String, theta:ProportionsVar, topics:Topics, entities:Entities,mentions:Mentions,assignments:Assignments,words:Words)


    //global model definitions

    implicit var model = DirectedModel()

    val beta = MassesVariable.growableUniform(entityDomain,0.1)
    val gamma = MassesVariable.growableUniform(mentionDomain,0.0)
    val delta = MassesVariable.growableUniform(wordDomain, 2000.0 / maxNrOfWords)
    val alphas = MassesVariable.dense(numTopics,50.0/numTopics)

    var phis = new Mixture[ProportionsVariable](for (i <- 1 to numTopics) yield
        new ProportionsVariable(new SparseTensorProportions1(new SparseIndexedTensor1(entityDomain.size))) ~ Dirichlet(beta))
    var psis = new Mixture[ProportionsVariable](for (i <- 1 to entityDomain.size) yield
        new ProportionsVariable(new SparseTensorProportions1(new SparseIndexedTensor1(mentionDomain.size))) ~ Dirichlet(gamma))
    var zetas = new Mixture[ProportionsVariable](for (i <- 1 to entityDomain.size) yield
        new ProportionsVariable(new SparseTensorProportions1(new SparseIndexedTensor1(wordDomain.size))) ~ Dirichlet(delta))

    def addVariablesToModel(words:Seq[String], entitiesAndMentions:Seq[(String,String)], name:String = ""):Document = {
        val theta = ProportionsVariable.dense(numTopics) ~ Dirichlet(alphas)
        val topics = new Topics(entitiesAndMentions.length) :~ PlatedDiscrete(theta)
        val entities = new Entities(name, theta, entitiesAndMentions.map(_._1)) ~ PlatedDiscreteMixture(phis, topics)
        val mentions = new Mentions(entitiesAndMentions.map(_._2), entities) ~ PlatedCategoricalMixture(psis, entities)

        val assignments = new Assignments(entities, words) :~ PlatedDiscreteFromInducedDistribution(entities)

        val ws = new Words(words) ~ PlatedCategoricalMixture(zetas, assignments)

        Document(name, theta, topics, entities, mentions, assignments, ws)
    }
    
    def clear() {
        model = DirectedModel()
    }

}

object PlatedDiscreteFromInducedDistribution extends DirectedFamily2[DiscreteSeqVariable,DiscreteSeqVariable] {
    case class Factor(override val _1:DiscreteSeqVariable, override val _2:DiscreteSeqVariable) extends super.Factor(_1, _2) with DiscreteGeneratingFactor {
        //def proportions: Proportions = _2 // Just an alias
        def pr(child:IndexedSeq[DiscreteValue], parent:IndexedSeq[DiscreteValue]) = {
            val group = parent.groupBy(i => i).mapValues(_.length.toDouble)
            child.map(i => group.getOrElse(i,0.0)  / parent.length).product
        }
        override def pr: Double = pr(_1.discreteValues,_2.discreteValues)
        override def prValue(intValue:Int): Double = _2.intValues.count(_ == intValue).toDouble/ _2.length
        override def sampledValue(implicit random: scala.util.Random): IndexedSeq[DiscreteValue] = sampledValue(_2.discreteValues)
        override def updateCollapsedParents(weight:Double): Boolean = throw new Error("Plated discrete parent cannot be collapsed.")

        def sampledValue(p1: IndexedSeq[DiscreteValue])(implicit random: Random): IndexedSeq[DiscreteValue] = _1.discreteValues.map(_ => p1(random.nextInt(p1.length)))
    }
    def newFactor(a:DiscreteSeqVariable, b:DiscreteSeqVariable) = {
        if (a.domain.elementDomain.size != b.domain.elementDomain.size) throw new Error("Discrete child domain size different from parent domain size.")
        Factor(a, b)
    }
}


