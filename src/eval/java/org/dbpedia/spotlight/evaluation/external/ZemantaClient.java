package org.dbpedia.spotlight.evaluation.external;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.string.XmlParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pablomendes
 */
public class ZemantaClient extends AnnotationClient {

    public static Log LOG = LogFactory.getLog(ZemantaClient.class);
    String api_key;

    public ZemantaClient(String api_key) {
        this.api_key = api_key;
    }

    /**
     *DISCLAIMER these are not really promised by Zemanta to be DBpediaEntities. We extract them from wikipedia links.
     * @param text
     * @return
     */
    public List<DBpediaResource> extract(Text text) {
        String response = process(text.text());
        List<DBpediaResource> entities = new ArrayList<DBpediaResource>();
        try {
            Element root = XmlParser.parse(response);
            String xpath = "//markup/links/link/target[type='wikipedia']/url";
            NodeList list = XmlParser.getNodes(xpath,root);
            LOG.info(String.format("Entities returned: %s", list.getLength()));
            for(int i=0; i<list.getLength(); i++) {
                Node n = list.item(i);
                String name = n.getNodeName();
                String value = n.getFirstChild().getNodeValue().replaceAll("http://en.wikipedia.org/wiki/","");
                //System.out.printf("Name:%s, Value: %s \n",name,value);
                entities.add(new DBpediaResource(value));
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return entities;
    }

    protected String process(String text) {
        String url = "http://api.zemanta.com/services/rest/0.0/";
        PostMethod method = new PostMethod(url);
        method.setRequestHeader("Content-type","application/x-www-form-urlencoded");

        // Grabbed from the bookmarklet
        NameValuePair[] params = {
                new NameValuePair("method","zemanta.suggest"),
                new NameValuePair("api_key",api_key),
                new NameValuePair("text",text),
                new NameValuePair("format","xml")
        };
        method.setRequestBody(params);
        LOG.debug("Sending request to Zemanta: "+params);


        String response = request(method);
        return response;
    }

    public static void main(String[] args) throws Exception {

        String api_key = args[0];
        ZemantaClient c = new ZemantaClient(api_key);

        File manualEvalInput = new File("/home/pablo/eval/manual/AnnotationText.txt");
        File manualEvalOutput = new File("/home/pablo/eval/manual/AnnotationText-Zemanta.txt.list");
       //c.evaluateManual(manualEvalInput, manualEvalOutput);

        File cucerzanEvalInput = new File("/home/pablo/eval/cucerzan/cucerzan.txt");
        File cucerzanEvalOutput = new File("/home/pablo/eval/cucerzan/systems/cucerzan-Zemanta.set");
        c.evaluateManual(cucerzanEvalInput, cucerzanEvalOutput);


//        ZemantaClient c = new ZemantaClient(api_key);
//        List<DBpediaResource> response = c.extract(new Text(text));
//        PrintWriter out = new PrintWriter(manualEvalDir+"AnnotationText-Zemanta.txt.set");
//        System.out.println(response);

    }
}
