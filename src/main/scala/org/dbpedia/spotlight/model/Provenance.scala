package org.dbpedia.spotlight.model

/**
 * Sources of Occurrences.
 */
object Provenance extends Enumeration
{
    val Wikipedia : Value = Value("Wikipedia")
    val Web : Value = Value("Web")
    val Annotation : Value = Value("Annotation")
    val Manual : Value = Value("Manual")    // constructed example (for testing etc.)
    val Undefined : Value = Value("Undefined")
//    class ProvenanceValue(name: String, var score : Double) extends Val(nextId, name)
//    protected final def Value(name: String, score : Double): ProvenanceValue = new ProvenanceValue(name,score)
}



