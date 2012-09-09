package org.dbpedia.spotlight.evaluation;

import org.dbpedia.spotlight.exceptions.SpottingException;
import org.dbpedia.spotlight.spot.CoOccurrenceBasedSelector;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SpotlightFactory;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.spot.Spotter;
import org.dbpedia.spotlight.spot.SpotterWithSelector;
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Joachim Daiber
 */
public class SpotterPerformanceEvaluator {


	public static void main(String[] args) throws InitializationException, ConfigurationException, IOException, JSONException, SpottingException {

		SpotlightConfiguration configuration = new SpotlightConfiguration("conf/server.properties");
		SpotlightFactory spotlightFactory = new SpotlightFactory(configuration);


		/**
		 * Read test corpus:
		 */

		BufferedReader bufferedReader = new BufferedReader(new FileReader(
				new File("/Users/jodaiber/Documents/workspace/ba/BachelorThesis/LaTeX/figures/performance/corpus.json")
		));
		String jsonString = bufferedReader.readLine();
		JSONObject jsonObject = new JSONObject(jsonString);


		///**
		// * No selection:
		// */
		Spotter spotter = new LingPipeSpotter(new File(configuration.getSpotterConfiguration().getSpotterFile()), configuration.getAnalyzer());
		//
		//extract(jsonObject, spotter);


		/**
		 * Advanced Spotter:
		 */
		Spotter spotterWithSelector = SpotterWithSelector.getInstance(
				spotter,
				new CoOccurrenceBasedSelector(configuration.getSpotterConfiguration()),
				spotlightFactory.taggedTokenProvider()
		);

		extract(jsonObject, spotterWithSelector);

	}

	private static void extract(JSONObject jsonObject, Spotter spotter) throws JSONException, SpottingException {
		Iterator keys = jsonObject.keys();
		while(keys.hasNext()) {
			String domain = (String) keys.next();
			JSONArray texts = jsonObject.getJSONArray(domain);
			List<SurfaceFormOccurrence> extracted = new LinkedList<SurfaceFormOccurrence>();

			long start = System.currentTimeMillis();
			for(int i = 0; i < texts.length(); i++) {
				extracted.addAll(spotter.extract(new Text(texts.getString(i))));
			}
			long end = System.currentTimeMillis();

			System.out.println("Domain: " + domain);
			System.out.println("# occurrences: " + extracted.size());
			System.out.println("Time: " + (end - start) + " ms");

		}
	}

}
