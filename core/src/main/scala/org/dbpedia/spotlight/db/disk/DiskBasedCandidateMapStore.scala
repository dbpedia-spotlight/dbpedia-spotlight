package org.dbpedia.spotlight.db.disk

import org.dbpedia.spotlight.model.{Candidate, SurfaceForm}
import org.dbpedia.spotlight.db.model.{ResourceStore, CandidateMapStore}

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class DiskBasedCandidateMapStore(file: String, resourceStore: ResourceStore) extends JDBMStore[Int, Array[Int]](file) with CandidateMapStore {

  override def get(surfaceForm: SurfaceForm): List[Candidate] =
    Option(super.get(surfaceForm.id)) match {
      case None => List[Candidate]()
      case Some(candidates) => candidates.map(
        resID => new Candidate(surfaceForm, resourceStore.get(resID))
      ).toList
    }

}
