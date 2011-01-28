package org.dbpedia.spotlight.model;

import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.vsm.FeatureVector;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: PabloMendes
 * Date: Jul 6, 2010
 * Time: 4:00:26 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ContextSearcher { //TODO We should have a ResourceSearcher, OccurrenceSearcher and DefinitionSearcher

    public List<FeatureVector> get(DBpediaResource resource) throws SearchException;

    //TODO this only makes sense for ResourceSearcher
    public DBpediaResource getDBpediaResource(int docNo) throws SearchException;

    public long getNumberOfEntries();

}
