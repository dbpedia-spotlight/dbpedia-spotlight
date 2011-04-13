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

import junit.framework.TestCase;
import org.dbpedia.spotlight.model.Text;

/**
 * HeadUpClient test case.
 *
 * @author jodaiber
 * 
 */
public class HeadUpClientTest extends TestCase {

	private AnnotationClient client;
	private Text text;

    public void setUp() throws Exception {
        super.setUp();
		client = new HeadUpClient();
		text = new Text("Google Inc. is an American multinational public corporation " +
				"invested in Internet search, cloud computing, and advertising " +
				"technologies. Google hosts and develops a number of Internet-based " +
				"services and products, and generates profit primarily from advertising " +
				"through its AdWords program.");
    }

    public void testExtract() throws Exception {
    	assertNotNull(client.extract(text));
    }

    public void testExtractReturnsFilledList() throws Exception {
    	assertNotSame(0, client.extract(text).size());
    }
	
  
}
