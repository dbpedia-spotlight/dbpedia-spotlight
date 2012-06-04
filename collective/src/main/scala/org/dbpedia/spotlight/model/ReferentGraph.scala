package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.model.{DBpediaResource, SurfaceFormOccurrence}
import spark.bagel._
import spark.bagel.Bagel._
import java.util.Enumeration

/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 6/2/12
 * Time: 4:27 PM
 */

/**
 * Construct a referent graph in Google Pregel graph with Bagel
 * There are
 * ->two kinds of vertices: entity (represented by DBpediaResource) and surfaceform
 * ->two kinds of edge: between entities and from surfaceform to entity
 * @author Hectorliu
 * @param sf2Cands
 */
class ReferentGraph(sf2Cands: Map[SurfaceFormOccurrence, List[DBpediaResource]]) {

  @scala.Serializable class RGVertex(val id: String, val vertexType: String, val vertexRef: AnyRef, val evidence: Double, val active: Boolean) extends Vertex

  @scala.Serializable class SFMessage(val targetId: String, val evidencePrpogationRatio: Double) extends Message

  val allCandidates = sf2Cands.foldLeft(List[DBpediaResource]()) {
    case (allCands, (sf, drs)) => {
      allCands ++ drs
    }
  }

  val entityVerts = allCandidates.map(res => {
    val id = res.toString


  })


}
