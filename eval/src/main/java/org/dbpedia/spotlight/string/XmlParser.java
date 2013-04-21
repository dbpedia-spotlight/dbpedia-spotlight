/**
 * Copyright 2011 Pablo Mendes, Max Jakob
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

package org.dbpedia.spotlight.string;

import com.sun.org.apache.xpath.internal.XPathAPI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author pablomendes
 */
public class XmlParser {

    public static Log LOG = LogFactory.getLog(XmlParser.class);

    public static Element parse(String xmlText) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbf.newDocumentBuilder();
		//LOG.debug("parsing"+xmlText);
		InputStream in = new ByteArrayInputStream(xmlText.getBytes("UTF-8"));
		Document doc = docBuilder.parse(in);

		Element rootTag = doc.getDocumentElement();
		LOG.debug(rootTag.getNodeName());
        return rootTag;
    }

    public static NodeList getNodes(String xpathExpr, Element rootTag){
		NodeList nodesFound = null;
		try
		{
			nodesFound = XPathAPI.selectNodeList(rootTag, xpathExpr);
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return nodesFound;
	}

}
