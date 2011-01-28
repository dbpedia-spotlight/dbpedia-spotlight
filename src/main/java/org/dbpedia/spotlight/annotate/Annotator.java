package org.dbpedia.spotlight.annotate;

import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.spot.Spotter;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;

import java.util.List;

/**
 * Annotation interface.
 *
 * Now in Java!
 */

public interface Annotator {

    public List<DBpediaResourceOccurrence> annotate(String text);

    public Disambiguator disambiguator();

    public Spotter spotter();

}
