package org.dbpedia.spotlight.spot

import org.dbpedia.spotlight.model.SurfaceFormOccurrence

/**
 * Interface for spot selectors (step after spotting and before disambiguation)
 *
 * @author pablomendes
 */
trait SpotSelector {
    /**
     * Takes in a list of spotted surface forms and returns a smaller list, only with the selected spot candidates
     */
    def select(occs: java.util.List[SurfaceFormOccurrence]) : java.util.List[SurfaceFormOccurrence];
}

/**
 * This interface requires tagged objects within the SurfaceFormOccurrence.
 * The @see{Spotter} is responsible for tagging and passing @see{TaggedText} objects downstream.
 */
trait TaggedSpotSelector extends SpotSelector

/**
 * This interface does not require tagged objects within the SurfaceFormOccurrence
 */
trait UntaggedSpotSelector extends SpotSelector