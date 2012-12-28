/**
 * Copyright 2011 DBpedia Spotlight 
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

package org.dbpedia.spotlight.web.rest.output;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.dbpedia.extraction.util.WikiUtil;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;

@XStreamAlias("resource")
public class Resource {

    @XStreamAsAttribute
    private String label;

    @XStreamAsAttribute
    private String uri;

    @XStreamAsAttribute
    private double contextualScore;

    @XStreamAsAttribute
    private double percentageOfSecondRank;

    @XStreamAsAttribute
    private int support;

    @XStreamAsAttribute
    private double priorScore;

    @XStreamAsAttribute
    private double finalScore;

    @XStreamAsAttribute
    private String types;

    public void setUri(String uri) {
        this.uri = /*DBpediaResource.DBPEDIA_RESOURCE_PREFIX() +*/ uri;
        this.label = WikiUtil.wikiDecode(uri);
    }

    public void setContextualScore(double contextualScore) {
        this.contextualScore = contextualScore;
    }

    public void setPercentageOfSecondRank(double percentageOfSecondRank) {
        this.percentageOfSecondRank = percentageOfSecondRank;
    }

    public void setSupport(int support) {
        this.support = support;
    }

    public void setPriorScore(double priorScore) {
        this.priorScore = priorScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = finalScore;
    }

    public void setTypes(String types) {
        this.types = types;
    }

    public static Resource getInstance(DBpediaResourceOccurrence occ) {
        Resource resource = new Resource();
        resource.setUri(occ.resource().uri());
        resource.setContextualScore(occ.contextualScore());
        resource.setPercentageOfSecondRank(occ.percentageOfSecondRank());
        resource.setSupport(occ.resource().support());
        resource.setPriorScore(occ.resource().prior());
        resource.setFinalScore(occ.similarityScore());
        resource.setTypes(occ.resource().types().mkString(", "));
        return resource;
    }

}
