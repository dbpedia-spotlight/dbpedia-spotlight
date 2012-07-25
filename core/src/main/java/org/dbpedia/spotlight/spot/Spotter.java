/*
 * Copyright 2011 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.spot;

import org.dbpedia.spotlight.exceptions.SpottingException;
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
	public List<SurfaceFormOccurrence> extract(Text text) throws SpottingException;

    /**
     * Every spotter has a name that describes its strategy
     * (for comparing multiple spotters during evaluation)
     * @return a String describing the Spotter configuration. e.g. TrieSpotter(test.TernaryIntervalSearchTree)
     */
    public String getName();

    /**
     * User should have the freedom to set the spotter name to something familiar to their use case
     */
    public void setName(String name);
	
}
