package org.dbpedia.spotlight.evaluation.external;

import junit.framework.TestCase;
import org.dbpedia.spotlight.model.Text;

import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 * @author Joachim Daiber
 */

public class AnnotationClientsTester extends TestCase {

	public static List<AnnotationClient> clients = new LinkedList<AnnotationClient>();
	static {

		clients.add(new ExtractivClient(null));
		clients.add(new HeadUpClient());
		clients.add(new DBpediaSpotlightClient());
		clients.add(new WikiMachineClient());
		clients.add(new WMWikifyClient());
		//clients.add(new AlchemyClient(API_KEY))
		//clients.add(new OntosClient(USERNAME,PASSWORD));

	}

	Text text = new Text("Google Inc. is an American multinational public corporation " +
			"invested in Internet search, cloud computing, and advertising " +
			"technologies. Google hosts and develops a number of Internet-based " +
			"services and products, and generates profit primarily from advertising " +
			"through its AdWords program.");


	public void testExtractReturnsFilledList() throws Exception {
		for (AnnotationClient client : clients) {
			assertNotSame("Failed with client " + client.getClass(), 0, client.extract(text).size());
		}
	}




}
