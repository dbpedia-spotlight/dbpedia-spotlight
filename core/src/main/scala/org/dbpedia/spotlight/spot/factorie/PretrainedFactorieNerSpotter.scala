package org.dbpedia.spotlight.spot.factorie

import cc.factorie.app.nlp.{DocumentAnnotationPipeline, Document, ner, DocumentAnnotatorPipeline}
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.model.{Feature, SurfaceForm, SurfaceFormOccurrence, Text}
import java.util

/**
 * See SimpleNerSpotter or BilouConllNerSpotter. Factorie provides different pretrained ner models.
 * To package them see pom.xml of core and change the scope of the artifact ner and pos to package (groupId: cc.factorie.app.nlp)
 */
class PretrainedFactorieNerSpotter(chainNer: DocumentAnnotationPipeline) extends Spotter {

  private def ontologyTypeFromLabel(label:String) = label.toLowerCase match {
    case "per" => "http://dbpedia.org/ontology/Person"
    case "org" => "http://dbpedia.org/ontology/Organisation"
    case "loc" => "http://dbpedia.org/ontology/Place"
    case "misc" => "SPOT"
    case _ => ""
  }

  def extract(text: Text) = {
    var doc = new Document(text.text)
    doc = chainNER.process(doc)

    val sfOccs = new util.ArrayList[SurfaceFormOccurrence]()

    def addOccurence(acc: (Int, Int, String)) {
      val sfOcc = new SurfaceFormOccurrence(new SurfaceForm(text.text.substring(acc._1, acc._2)), text, acc._1)
      if(acc._3 != "SPOT")
        sfOcc.setFeature(new Feature("ontology-type", acc._3))
      sfOccs.add(sfOcc)
    }

    doc.tokens.foldLeft((-1,-1,""))((acc,token) => {
      val label = token.nerTag.shortCategoryValue
      val typ = ontologyTypeFromLabel(label)
      if(acc._1 < 0) {
        if(typ.isEmpty) acc
        else (token.stringStart, token.stringEnd, typ)
      } else {
        if(typ.isEmpty){
          addOccurence(acc)
          (-1,-1,"")
        } else {
          if(typ == acc._3)
            (acc._1,token.stringEnd, typ)
          else {
            addOccurence(acc)
            (token.stringStart, token.stringEnd, typ)
          }
        }
      }
    })

    sfOccs
  }

  def getName = name
  private var name  = "Pretrained CONLL Ner Tagger"

  def setName(name: String) = this.name = name

  def main(args:Array[String]) {
    val occs = extract(new Text("New york is a big city in the USA!"))
    occs
  }
}

/**
 * Fast, but not as sophisticated as BilouConllPretrainedCRFSpotter
 */
object SimpleNerSpotter extends PretrainedFactorieNerSpotter(DocumentAnnotatorPipeline.apply[ner.NerTag])


/**
 * Slow but uses many features for NERTagging and is thus much more sophisticated compared to the SimpleNerSpotter
 */
object BilouConllNerSpotter extends PretrainedFactorieNerSpotter(DocumentAnnotatorPipeline.apply[ner.BilouConllNerTag])


