package org.dbpedia.spotlight.disambiguate;

import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.*;

import java.util.List;

/**
 * Interface for disambiguators.
 */

public interface Disambiguator {

    public List<SurfaceFormOccurrence> spotProbability(List<SurfaceFormOccurrence> sfOccurrences) throws SearchException;

    public List<DBpediaResourceOccurrence> disambiguate(List<SurfaceFormOccurrence> sfOccurrences) throws SearchException; //TODO DisambiguationException

    public List<DBpediaResourceOccurrence> bestK(SurfaceFormOccurrence sfOccurrence, int k) throws SearchException, ItemNotFoundException;

    /**
     * Every disambiguator has a name that describes its settings (used in evaluation to compare results)
     * @return a short description of the Disambiguator
     */
    public String name();

    /**
     * Every disambiguator should know how to measure the ambiguity of a surface form.
     * @param sf
     * @return ambiguity of surface form (number of surrogates)
     */
    public int ambiguity(SurfaceForm sf) throws SearchException;

    /**
     * Counts how many occurrences we indexed for a given URI. (size of training set for that URI)
     * @param resource
     * @return
     * @throws SearchException
     */
    public int trainingSetSize(DBpediaResource resource) throws SearchException;
}