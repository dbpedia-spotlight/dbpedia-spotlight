package org.dbpedia.spotlight.spot;

import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;

import java.util.List;

/**
 * A Spotter recognizes occurrences of {@link org.dbpedia.spotlight.model.SurfaceForm} in some source {@link org.dbpedia.spotlight.model.Text}
 * @author PabloMendes
 */
public interface Spotter {

	/**
	 * Extracts a set of surface form occurrences from the text
	 */
	public List<SurfaceFormOccurrence> extract(Text text);

    /**
     * Every spotter has a name that describes its strategy
     * (for comparing multiple spotters during evaluation)
     * @return a String describing the Spotter configuration. e.g. TrieSpotter(test.TernaryIntervalSearchTree)
     */
    public String name();
	
}
