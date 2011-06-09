package org.dbpedia.spotlight.tagging.lingpipe;

import junit.framework.TestCase;
import org.dbpedia.spotlight.model.SpotlightFactory;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.tagging.TextUtil;

/**
 * @author Joachim Daiber
 *
 */
public class LingPipeTextUtilTest extends TestCase {

	private TextUtil textUtil;

	String text = "Aguri Suzuki, a 44-year-old real estate agent, says she sometimes " +
		"thinks the ground is shaking even when it is not. When she sees a tree branch swaying in the wind, " +
		"she worries there has been an earthquake. Doctors here say they are seeing more people who are " +
		"experiencing such phantom quakes, as well as other symptoms of “earthquake sickness” " +
		"like dizziness and anxiety.";

    public LingPipeTextUtilTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();

		SpotlightFactory luceneFactory = new SpotlightFactory(new SpotlightConfiguration("conf/server.properties"));
		textUtil = luceneFactory.textUtil();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetSentence() throws Exception {
        assertEquals("Aguri Suzuki, a 44-year-old real estate agent, says she sometimes " +
				"thinks the ground is shaking even when it is not.",
				textUtil.getSentence(28, 45, text));
    }

	 public void testGetSentence2() throws Exception {
        assertEquals("When she sees a tree branch swaying in the wind, she worries there has been an earthquake.",
				textUtil.getSentence(122, 125, text));

    }


}
