/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.annotate;

import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguator;
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.spot.Spotter;

import java.util.List;

/**
 * Interface for document-centric annotators.
 */

public interface ParagraphAnnotator {

    //TODO should this return AnnotatedText instead?
    public List<DBpediaResourceOccurrence> annotate(String text) throws SearchException, InputException;

    //TODO this java/scala incompatibility here has to be worked out
    public ParagraphDisambiguatorJ disambiguator();

    public Spotter spotter();

}
