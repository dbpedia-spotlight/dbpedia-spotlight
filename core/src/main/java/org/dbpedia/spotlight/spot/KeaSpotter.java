/*
 * Copyright 2011 DBpedia Spotlight Development Team and Goss Interactive Ltd.
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.spot.Spotter;
import com.gossinteractive.kea.KeaKeyphraseExtractorAPI;
import com.gossinteractive.kea.KeaModelBuilderAPI;
import com.gossinteractive.kea.KeyPhrase;

/**
 * Based on Kea (http://www.nzdl.org/Kea/) a keyphrase extractor.
 * Extracts keyphrases in order to produce spots to be disambiguated by DBpedia Spotlight.
 *
 * @author Rohana Rajapakse (GOSS Interactive Limited) - implemented the class
 */
public class KeaSpotter implements Spotter {

	private final Log LOG = LogFactory.getLog(this.getClass());

	String keamodelfile = null;

    int nofPhrases = 100;
    double cutoff = 0.075;


	public KeaSpotter(String modelfile) {
		keamodelfile = modelfile;
	}

    public KeaSpotter(String modelfile, int maxNumberOfPhrases, double cutOff) {
        keamodelfile = modelfile;
        this.nofPhrases = maxNumberOfPhrases;
        this.cutoff = cutOff;
    }

	@Override
	public List<SurfaceFormOccurrence> extract(Text intext) {

		LOG.debug("\n\nRR- extract(...) method called! with text: " + intext + "\n\n");
		List<SurfaceFormOccurrence> sfOccurrences = new ArrayList<SurfaceFormOccurrence>();
		try {

			String text = intext.text();
			List<SurfaceFormOccurrence> ret = null;
			//Extracting keyphrase spots
			KeaKeyphraseExtractorAPI kmb = new KeaKeyphraseExtractorAPI(keamodelfile,nofPhrases );

            //TODO add configuration parameters to set number of keyphrases or minimum cutoff
			List<KeyPhrase> keaPhrases = kmb.extractKeyphrases3(text, cutoff); //extracts phrases with weight > cutoff
			//List<KeyPhrase> keaPhrases = kmb.extractKeyphrases3(text, nofPhrases); //extracts a given no. of phrases
			
            //System.out.println("\n\nNo of Kea Keyphrases extracted: " + keaPhrases.size());
			for( KeyPhrase kp: keaPhrases) {
				//System.out.println("KP: " + kp.getPhrase() + " ( Weight: " + kp.getWeight() + " Rank: " + kp.getRank() + ")");
				LOG.debug("Occurrences of kp " + kp.getPhrase() + " are: " + kp.getOffsetslist());
				SurfaceForm surfaceForm = new SurfaceForm(kp.getPhrase());
				for (Integer s: kp.getOffsetslist()) {
					SurfaceFormOccurrence sfocc =  new SurfaceFormOccurrence(surfaceForm, intext, s);
					sfOccurrences.add(sfocc);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return sfOccurrences;
		}
	}

    String name = "KeaSpotter";

	@Override
	public String getName() {
		return this.name;
	}

    public void setName(String n) {
        this.name =  n;
    }

	public static void main( String[] args ) {
//		String keamodelfile = "/data/spotlight/3.7/kea/keaModel-1-3-1";
//		String trainingFilePath = "/data/spotlight/3.7/kea/trainingdata";
//		KeaModelBuilderAPI kmb = new KeaModelBuilderAPI();
//		try {
//			kmb.buildKeaModel(keamodelfile, trainingFilePath, 100, 1, 3, 1);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

        String keamodelfile = "/data/spotlight/3.7/kea/keaModel-1-3-1";
		KeaSpotter ks = new KeaSpotter(keamodelfile);

		String intext = "The last few decades have seen fundamental changes in food consumption patterns around the world. These changes were characterized not only by an increase in overall calorie intakes but also by a shift in the composition of the diet towards more meat, eggs, dairy products as well as more fats and oils, i.e. a shift towards high calorie diets that are also much richer in saturated fats and cholesterol. The main drivers of this transition include factors such as: (i) rapidly falling real prices for food; (ii) urbanization with the development of new marketing channels and the spread of supermarkets into developing countries; (iii) and freer trade and globalization with the emergence of large, trans-nationally operating food companies. This diet transition also brought about a rapid increase in the prevalence of overweight, obesity and related non-communicable diseases (NCDs). Initially, these problems were limited to developed countries, but more recently, there are growing concerns that the adverse effects of a rapid nutrition transition could even be more severe in developing countries. The growing health concerns have also given rise to a intense debate about possible remedies to stop and reverse the obesity epidemic in developed countries, and, perhaps even more importantly, to prevent similar developments in developing countries. Some of these policy options are being examined in this paper. The instruments analysed include price interventions, both at the level of primary commodities and final consumer goods (tax on fat food), direct incentives to reduce and disincentives to maintain an excess body weight; finally the paper also presents some experience gathered with a combination of various measures in integrated nutrition programmes. ";

		List<SurfaceFormOccurrence> sflst = ks.extract(new Text(intext));
		System.out.println("\n\nPrinting SurfaceOccurrences");
		for (SurfaceFormOccurrence so: sflst) {
			System.out.println(so.surfaceForm().name() + " : offset " + so.textOffset());
		}
	}
	
}
