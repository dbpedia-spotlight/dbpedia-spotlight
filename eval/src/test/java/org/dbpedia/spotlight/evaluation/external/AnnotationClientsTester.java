package org.dbpedia.spotlight.evaluation.external;

import junit.framework.TestCase;
import org.dbpedia.spotlight.model.Text;

import java.util.LinkedList;
import java.util.List;

/**
 * The External Clients were translated to Scala but this class was not.
 * Because some of the services are working (no longer exist, unavailable) this tester was not deleted.
 * But it contain now only the calling for the not tanslated clients.
 * As result of client's problems, this tester is no more working.
 *
 * There are no AnnotationClientsTester implemented in scala yet. One can test each client running its scala class.
 * They can be found at: eval/src/main/java/org/dbpedia/spotlight/evaluation/external/
 *
 * Last Tested: 08/27th/2013 by Alexandre Cançado Cardoso
 */

/**
 * @author Joachim Daiber
 */

public class AnnotationClientsTester extends TestCase {

	public static List<AnnotationClient> clients = new LinkedList<AnnotationClient>();
	static {

		clients.add(new ExtractivClient(null));
		clients.add(new HeadUpClient());
        //clients.add(new OntosClient(USERNAME,PASSWORD));
		clients.add(new WikiMachineClient());
		clients.add(new WMWikifyClient());

        //The clients below were translated to scala, and the java clients were deleted from the project
        //clients.add(new DBpediaSpotlightClient()); // Scala client at DBpediaSpotlightClientScala.scala
		//clients.add(new AlchemyClient(API_KEY)); // Scala client at AlchemyClientScala.scala
        //clients.add(new OpenClalaisClient(API_KEY_OC)); // Scala client at OpenCalaisClientScala.scala
        //clients.add(new ZemantaClient(API_KEY_OC)); // Scala client at ZemantaClientScala.scala
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
