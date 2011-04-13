/**
 * Copyright 2011 Pablo Mendes, Max Jakob, Joachim Daiber
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


package org.dbpedia.spotlight.evaluation.external;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.Text;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

/**
 * @author jodaiber
 *
 * This is an AnnotationClient for HeadUpClient (http://www.headup.com/).
 *
 * The Query syntax consists of a number of piped commands,
 * entity extraction can be applied on plain text:
 *
 *  http://api.headup.com/v1?q=/str('France, which has over 1,500 ...')/x:entity
 *
 * or on websites:
 *
 *  http://api.headup.com/v1?q=/str('http://www.nytimes.com/2011/03/13/world/asia/13nuclear.html')/x:entity
 *
 * Features and ease of use:
 *
 * - currently no API key required
 * - for entities, the API returns a list of surface forms with their corresponding surface form
 *   in the text
 * - DBPedia entites are identified by the dbpedia: prefix
 * 
 * 
 */

public class HeadUpClient extends AnnotationClient {

	final static String API_URL = "http://api.headup.com/v1?q=";

	@Override
	public List<DBpediaResource> extract(Text text) throws AnnotationException {

		LinkedList<DBpediaResource> dbpediaResources = new LinkedList<DBpediaResource>();

		JSONArray requestData;
		try {
			requestData = request(buildURL(text));
		} catch (JSONException e) {
			throw new AnnotationException("Could not read API response.");
		} catch (UnsupportedEncodingException e) {
			throw new AnnotationException("Could not build API request URL.");
		}

		if(requestData.length() == 0)
			return dbpediaResources;

		for(int i = 0; i < requestData.length(); i++) {
			try {
				DBpediaResource dBpediaResource
						= new DBpediaResource(requestData.getJSONObject(i).getString("uri").replace("dbpedia:", ""));
				dbpediaResources.add(dBpediaResource);
			} catch (JSONException e) {
				
			}
		}

		return dbpediaResources;
	}

	
	private String buildURL(Text text) throws UnsupportedEncodingException {
		return API_URL + "/str('" + URLEncoder.encode(text.text(), "utf-8") + "')/x:entity";
	}


	private JSONArray request(String url) throws JSONException {
		GetMethod method = new GetMethod(url);
		String response = request(method);
		JSONArray json = new JSONArray(response);
		return json;
	}


}
