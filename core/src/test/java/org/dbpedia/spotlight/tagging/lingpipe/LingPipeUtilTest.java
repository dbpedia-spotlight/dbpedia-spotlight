package org.dbpedia.spotlight.tagging.lingpipe;

import com.aliasi.sentences.IndoEuropeanSentenceModel;
import junit.framework.TestCase;

/**
 * @author Joachim Daiber
 *
 */
public class LingPipeUtilTest extends TestCase {

	String text = "Aguri Suzuki, a 44-year-old real estate agent, says she sometimes " +
		"thinks the ground is shaking even when it is not. When she sees a tree branch swaying in the wind, " +
		"she worries there has been an earthquake. Doctors here say they are seeing more people who are " +
		"experiencing such phantom quakes, as well as other symptoms of “earthquake sickness” " +
		"like dizziness and anxiety.";

    public LingPipeUtilTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();	
		LingPipeFactory.setSentenceModel(new IndoEuropeanSentenceModel());
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetSentence() throws Exception {
        assertEquals("Aguri Suzuki, a 44-year-old real estate agent, says she sometimes " +
				"thinks the ground is shaking even when it is not.",
				LingPipeUtil.getSentence(28, 45, text));
    }

	 public void testGetSentence2() throws Exception {
        assertEquals("When she sees a tree branch swaying in the wind, she worries there has been an earthquake.",
				LingPipeUtil.getSentence(122, 125, text));

    }


}
