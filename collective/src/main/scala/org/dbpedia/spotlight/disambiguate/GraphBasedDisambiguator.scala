package org.dbpedia.spotlight.disambiguate

import java.util.List
import org.dbpedia.spotlight.disambiguate.{ParagraphDisambiguator, Disambiguator}
import org.dbpedia.spotlight.model.SurfaceFormOccurrence

/*
 * Copyright 2012 DBpedia Spotlight Development Team
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

/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 5/30/12
 * Time: 3:56 PM
 */

class GraphBasedDisambiguator extends Disambiguator with ParagraphDisambiguator {
  def spotProbability(sfOccurrences: List[SurfaceFormOccurrence]) = {

  }
}
