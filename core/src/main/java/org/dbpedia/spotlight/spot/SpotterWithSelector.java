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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.annotate.DefaultAnnotator;
import org.dbpedia.spotlight.exceptions.SpottingException;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.tagging.TaggedTokenProvider;

import java.util.List;

/**
 * Wrapper class combining spotting and spot selection.
 *
 * @author Joachim Daiber
 * @author pablomendes
 */
public abstract class SpotterWithSelector implements Spotter {

	private final Log LOG = LogFactory.getLog(DefaultAnnotator.class);

	protected Spotter spotter;
	protected SpotSelector spotSelector;

    protected String name;

	public static SpotterWithSelector getInstance(Spotter spotter, UntaggedSpotSelector spotSelector) {
		return new UntaggedSpotterWithSelector(spotter, spotSelector);
	}

	public static SpotterWithSelector getInstance(Spotter spotter, TaggedSpotSelector spotSelector, TaggedTokenProvider tagger) {
		return new TaggedSpotterWithSelector(spotter, spotSelector, tagger);
	}

	protected abstract Text buildText(Text text);

    /**
     * Applies the base spotter specified, then applies the selector on the generated spots.
     * Before applying the spotter it uses the abstract method buildText to create a Text object.
     * As a result, it will first POS tag the sentence if this is a TaggedSpotSelector,
     *
     * @param text
     * @return
     * @throws SpottingException
     */
	public List<SurfaceFormOccurrence> extract(Text text) throws SpottingException {
        LOG.debug(String.format("Spotting with spotter %s and selector %s.",spotter.getName(),spotSelector));

		Text textObject = buildText(text);

		List<SurfaceFormOccurrence> spottedSurfaceForms = spotter.extract(textObject);

		if(spotSelector != null) {
			List<SurfaceFormOccurrence> selectedSpots = spotSelector.select(spottedSurfaceForms);

			LOG.info("Selecting candidates...");
			int previousSize = spottedSurfaceForms.size();
			int count = previousSize - selectedSpots.size();
			String percent = (count == 0) ? "0" : String.format("%1.0f", (((double) count) / previousSize) * 100);
			LOG.info(String.format("Removed %s (%s percent) spots using spotSelector %s", count, percent, this.spotSelector.getClass().getSimpleName()));

			return selectedSpots;

		}else{
			return spottedSurfaceForms;
		}

	}

    public String getName() {
        if (this.name==null || this.name.equals("")) {
            String newName = "SpotterWithSelector:"+spotter.getName();
            if (spotSelector!=null) newName += ", " + spotSelector.getClass().toString();
            return newName;
        } else {
            return this.name;
        }
	}

    public void setName(String n) {
        this.name = n;
    }

	protected static class TaggedSpotterWithSelector extends SpotterWithSelector {
		private TaggedTokenProvider tagger = null;
		public TaggedSpotterWithSelector(Spotter spotter, SpotSelector spotSelector, TaggedTokenProvider tagger) {
			this.spotter = spotter;
			this.spotSelector = spotSelector;
			this.tagger = tagger;
		}
		@Override
		protected Text buildText(Text text) {
			if(text instanceof TaggedText)
				return text;
			else
				return new TaggedText(text.text(), tagger);
		}
	}

	protected static class UntaggedSpotterWithSelector extends SpotterWithSelector {
		public UntaggedSpotterWithSelector(Spotter spotter, SpotSelector spotSelector) {
			this.spotter = spotter;
			this.spotSelector = spotSelector;
		}
		@Override
		protected Text buildText(Text text) {
			return text;
		}
	}
}
