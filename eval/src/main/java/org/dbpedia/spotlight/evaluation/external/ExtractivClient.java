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

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.*;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.DBpediaType;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.Text;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * The External Clients were translated to Scala but this class was not.
 * Because the Extrectiv service (http://extractiv.com/) is no longer available. As result of that, this client is no more working.
 *
 * Last Tested: 08/27th/2013 by Alexandre Cançado Cardoso
 */

/**
 * This is a simple Annotation Client for Extractiv.com
 *
 * Extractiv.com offers three account levels, the most basic of which is
 * free and allows to process up to 1000 documents per day.
 *
 * Extractiv.com uses 157 entity categories, most of which can be mapped
 * onto the DBPedia class hierarchy. These categories include 34 categories
 * for pattern-based entities like dates, telephone numbers and URLs.
 *
 * An extensive Java interface is available at
 * - https://github.com/extractiv/ExtractivPublicCode/
 *
 */

public class ExtractivClient extends AnnotationClient {

	private final static String API_URL = "http://rest.extractiv.com/extractiv/json/";
	private String apiKey;

	private static Properties typeMapping;


	public ExtractivClient(String apiKey) {
		this.apiKey = apiKey;


		Properties properties = new Properties();
		try {
			properties.load(this.getClass().getResourceAsStream("/ExtractivEntityTypes.properties"));
		} catch (IOException e) {
			LOG.error("Could not load Extractiv Entity Type mapping.");
		}
		typeMapping = properties;
	}

	@Override
	public List<DBpediaResource> extract(Text text) throws AnnotationException {

		List<DBpediaResource> extractedResources = new ArrayList<DBpediaResource>();

		final URI extractivServerURI;
		try {
			extractivServerURI = new URI(API_URL);
		} catch (URISyntaxException e) {
			throw new AnnotationException(e);
		}

		final HttpMethodBase extractivRequest;

		File tmp = null;
		try {
			tmp = File.createTempFile("tmpText", ".txt");
			FileWriter fileWriter = new FileWriter(tmp);
			fileWriter.write(text.text());
			fileWriter.close();
			extractivRequest = getExtractivProcessFileRequest(extractivServerURI, tmp);
		} catch (IOException e) {
			throw new AnnotationException("Could not create request for Extractiv API.");
		}

		final String extractivResults = request(extractivRequest);
		JSONObject resultsJSON = null;
		JSONArray entities = null;

		try {
			resultsJSON = new JSONObject(extractivResults);
			entities = resultsJSON.getJSONArray("entities");
		} catch (JSONException e) {
			throw new AnnotationException("Received invalid response from Extractiv API.");
		}

		for (int i = 0; i < entities.length(); i++) {
			try {
				JSONObject entity = (JSONObject) entities.get(i);

				if (!entity.has("links"))
					continue;

				JSONArray links = entity.getJSONArray("links");

				String dbpediaLink = null;
				for (int j = 0; j < links.length(); j++) {
					if (links.getString(j).startsWith(SpotlightConfiguration.DEFAULT_NAMESPACE)) {
						dbpediaLink = links.getString(j);
					}
				}

				if (dbpediaLink == null)
					continue;

				List<DBpediaType> dBpediaTypes = new LinkedList<DBpediaType>();

				String extractivType = entity.getString("type");
				String dbpediaType = (String) typeMapping.get(extractivType);

				if (dbpediaType == null || dbpediaType.equals("NO_MATCH")) {
					continue;
				}

				dBpediaTypes.add(new DBpediaType(dbpediaType));
				DBpediaResource dBpediaResource = new DBpediaResource(dbpediaLink, 0);

				if (!dbpediaType.equals("http://www.w3.org/2002/07/owl#Thing")) {
					dBpediaResource.setTypes(dBpediaTypes);
				}

				extractedResources.add(dBpediaResource);
			} catch (JSONException e) {
				
			}
		}


		return extractedResources;

	}


	/**
	 * Adapted from
	 *
	 * https://github.com/extractiv/ExtractivPublicCode/blob/master/src/main/java/com/extractiv/rest/RESTDemo.java
	 *
	 *
	 * Generates a HttpMethodBase that will request the given file to be processed by the Extractiv annotation service.
	 *
	 * @param extractivURI The URI of the Extractiv annotation service
	 * @param file		 The file to process
	 */
	private PostMethod getExtractivProcessFileRequest(final URI extractivURI, final File file)
			throws FileNotFoundException {

		final PartBase filePart = new FilePart("content", file, "multipart/form-data", null);

		// bytes to upload
		final ArrayList<Part> message = new ArrayList<Part>();
		message.add(filePart);
		message.add(new StringPart("formids", "content"));
		message.add(new StringPart("output_format", "JSON"));
		if (apiKey != null) {
			message.add(new StringPart("api_key", apiKey));
		}

		final Part[] messageArray = message.toArray(new Part[0]);

		// Use a Post for the file upload
		final PostMethod postMethod = new PostMethod(extractivURI.toString());
		postMethod.setRequestEntity(new MultipartRequestEntity(messageArray, postMethod.getParams()));

		return postMethod;

	}


}
