package org.dbpedia.spotlight.model;

import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: PabloMendes
 * Date: Jul 6, 2010
 * Time: 4:00:26 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SurrogateSearcher {

    public Set<DBpediaResource> get(SurfaceForm sf) throws SearchException, ItemNotFoundException;

}
