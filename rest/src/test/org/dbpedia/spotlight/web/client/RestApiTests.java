/*
 * Copyright 2011 Pablo Mendes, Max Jakob
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

package org.dbpedia.spotlight.web.client;
import java.io.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Class that tests DBpedia Spotlight's REST API with two different clients (HTTPClient and Jersey Client)
 *
 * @author Giuseppe Rizzo
 * @author pablomendes (refactored code,small fixes, transformed into jUnit tests)
 */
public class RestApiTests {

	@Test
	public void productionWithShortText() {
	       jersey_client(shortText,production,"Default");
	       http_client(shortText,production,"Default");
	}

	@Test
	public void devDocumentCentricWithShortText() {
	       jersey_client(shortText,dev,"Document");
	       http_client(shortText,dev,"Document");
	}

	@Test
	public void devOccurrenceCentricWithShortText() {
	       jersey_client(shortText,dev,"Occurrences");
	       http_client(shortText,dev,"Occurrences");
	}

	@Test
	public void productionWithLongText() {
	       jersey_client(longText,production,"Default");
	       http_client(longText,production,"Default");
	}

	@Test
	public void devDocumentCentricWithLongText() {
	       String json1 = jersey_client(longText,dev,"Document");
	       String json2 = http_client(longText,dev,"Document");

           assertEquals(json1,json2);
	}

	@Test
	public void devOccurrenceCentricWithLongText() {
	       jersey_client(longText,dev,"Occurrences");
	       http_client(longText,dev,"Occurrences");
	}

    //TODO refactor method as AnnotationClient subclass, move to core
	private static String http_client(String text, String url, String disambiguator)
	{
        System.out.println("*** HTTP Client");
		HttpClient client = new HttpClient();
	    PostMethod method = new PostMethod(url);
	    method.setRequestHeader("ContentType","application/x-www-form-urlencoded;charset=UTF-8");
	    method.setRequestHeader("Accept", "application/json");
	    method.addParameter("disambiguator", disambiguator);
	    method.addParameter("confidence", "-1");
	    method.addParameter("support", "-1");
	    method.addParameter("text", text);

	    // Send POST request
        StringBuffer jsonString = new StringBuffer();
	    int statusCode;
		try {
			statusCode = client.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
	        	System.err.println("Method failed: " + method.getStatusLine());
	        }

		    InputStream rstream = null;
		    rstream = method.getResponseBodyAsStream();

	        BufferedReader br = new BufferedReader(new InputStreamReader(rstream));
	        String line;

	        while ((line = br.readLine()) != null) {
	            jsonString.append(line);
                jsonString.append("\n");
	        }
	        System.out.println(jsonString);
	        br.close();
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return jsonString.toString();
	}

    //TODO refactor method as AnnotationClient subclass, move to core
	private static String jersey_client(String text, String url, String disambiguator) {
		System.out.println("*** Jersey Client");
        Client client = Client.create();
        WebResource webResource = client.resource(url);
        String json = "";
        try {
            MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
            queryParams.add("disambiguator", disambiguator);
            queryParams.add("confidence", "-1");
            queryParams.add("support", "-1");
            queryParams.add("text", text);
            //queryParams.add("text", java.net.URLEncoder.encode(text,"UTF-8"));


            json = webResource.
                    accept( MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).
                    header("ContentType","application/x-www-form-urlencoded;charset=UTF-8").
                    //type(MediaType.APPLICATION_FORM_URLENCODED).
                    //type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).
                            post(String.class, queryParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(json);
        return json.concat("\n");
	}

	private static void length_test(String text, String url)
	{
		 for(int i=0; i<100; i++)
	        {
	        	System.out.println("#" + i + " " + text.length()/1024 + "KB");

	            text += text;
	        }
	}

	private static String production = "http://spotlight.dbpedia.org/rest/annotate";
	private static String dev = "http://spotlight.dbpedia.org/dev/rest/annotate";
    //private static String dev = "http://localhost:2223/rest/annotate";

	private static String shortText = "Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
	"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
	"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis ";
	private static String longText = "Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists" +
			"Agathe Habyarimana, 69, is accused by the Rwandan authorities " +
			"of helping to plan the genocide. She denies the accusations. French forces flew her out of Rwanda " +
			"shortly after the violence began and she has lived in France for years. More than 800,000 Tutsis " +
			"and moderate Hutus died in the massacres. Juvenal Habyarimana, a Hutu, was killed when " +
			"his plane was shot down above Kigali airport on 6 April 1994. Within hours a campaign of violence, " +
			"carried out mostly by Hutus against Tutsis, spread from the capital throughout the country. The " +
			"Hutu militias blamed the Tutsis for downing the president's plane, although it has never " +
			"been proved who was responsible. It is widely believed that Hutu extremists and the " +
			"government had long planned the genocide. Continue reading the main story Rocky relations " +
			"After the Paris court gave its judgement, Mrs Habyarimana told journalists";

	private static String encodedText(String text) throws UnsupportedEncodingException {
        return java.net.URLEncoder.encode(text,"UTF-8");
    }
}
