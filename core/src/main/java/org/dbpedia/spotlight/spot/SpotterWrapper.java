/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dbpedia.spotlight.spot;

import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.tagging.TaggedTokenProvider;

import java.util.List;

/**
 * Wrapper class combining spotting and spot selection.
 *
 * This class takes a Spotter and a Spot selector
 *
 *
 * @author Joachim Daiber
 * @author pablomendes
 */
public abstract class SpotterWrapper implements Spotter {

    protected Spotter spotter;
    protected SpotSelector spotSelector;


    public SpotterWrapper getInstance(Spotter spotter, UntaggedSpotSelector spotSelector) {
        this.spotter = spotter;
        this.spotSelector = spotSelector;
        return new UntaggedSpotterWrapper(spotter, spotSelector);
    }

    public SpotterWrapper getInstance(Spotter spotter, TaggedSpotSelector spotSelector, TaggedTokenProvider tagger) {
        return new TaggedSpotterWrapper(spotter, spotSelector, tagger);
    }

    protected abstract Text buildText(Text text);

    public List<SurfaceFormOccurrence> extract(Text text) {

        Text textObject = buildText(text);

        List<SurfaceFormOccurrence> spottedSurfaceForms = spotter.extract(textObject);

        if(spotSelector != null) {
            return spotSelector.select(spottedSurfaceForms);
        }else{
            return spottedSurfaceForms;
        }

    }

    public String name() {
        String name = "SpotterWrapper:"+spotter.name();
        if (spotSelector!=null) name+= spotSelector.getClass().toString();
        return name;
    }

    protected class TaggedSpotterWrapper extends SpotterWrapper {
        private TaggedTokenProvider tagger = null;
        public TaggedSpotterWrapper(Spotter spotter, SpotSelector spotSelector, TaggedTokenProvider tagger) {
            this.spotter = spotter;
            this.spotSelector = spotSelector;
            this.tagger = tagger;
        }
        @Override
        protected Text buildText(Text text) {
            return new TaggedText(text.text(), tagger);
        }
    }

    protected class UntaggedSpotterWrapper extends SpotterWrapper {
        public UntaggedSpotterWrapper(Spotter spotter, SpotSelector spotSelector) {
            this.spotter = spotter;
            this.spotSelector = spotSelector;
        }
        @Override
        protected Text buildText(Text text) {
            return text;
        }
    }
}
