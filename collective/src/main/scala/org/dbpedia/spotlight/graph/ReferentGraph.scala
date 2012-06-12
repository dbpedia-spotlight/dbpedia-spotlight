package org.dbpedia.spotlight.graph

import it.unimi.dsi.law.rank.{PageRank, PageRankPowerMethod}
import it.unimi.dsi.webgraph.ImmutableGraph
import it.unimi.dsi.webgraph.ArrayListMutableGraph

import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{DBpediaResource, SurfaceFormOccurrence}

/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 6/2/12
 * Time: 4:27 PM
 */

/**
 * Construct a referent graph described in Han to compute the evidence population
 * This graph can be viewed as a subgraph extracted from the graph of all entities
 * There are
 * ->two kinds of vertices: entity (represented by DBpediaResource) and surfaceform
 * ->two kinds of edge: between entities of different surfaceforms and from surfaceform to entity
 * @author Hectorliu
 * @param sf2Cands
 */
class ReferentGraph(sf2Cands: Map[SurfaceFormOccurrence, List[DBpediaResource]]) {
  val graphTopo = sf2Cands.foldLeft(List[DBpediaResource]()) {
    case (cands, (sf, drs)) => {
      cands ++ drs
    }
  }

  /**
   * Get initial evidence for each mention, which may differ from its context
   */
  def getInitalEvidence(sfOcc: SurfaceFormOccurrence, cand: DBpediaResource) {

  }

  /**
   * Get sementaic weight between entities
   * Extract from the Wikipedia Link structure
   */
  def getEntitySemanticLinks() {

  }

  /**
   * Get compatibility score between entitty and mention
   * Current implementation consider TF.ICF
   */
  def getMentionEntityCompatibility() {


  }


}
