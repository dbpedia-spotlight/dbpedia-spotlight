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

package org.dbpedia.spotlight.evaluation.external;

import net.sf.json.JSONObject;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.AnnotationException;
import org.dbpedia.spotlight.model.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This External Clients was partly tranlated to scala, wich has a bug to be fixed.
 * The buged scala code can be found at: https://github.com/accardoso/dbpedia-spotlight/tree/dev/conv/external_clients/eval/src/main/scala/org/dbpedia/spotlight/evaluation/external
 * As result of that, this java class still be the unique client for OpenCalais
 *
 * Last Tested: 08/28th/2013 by Alexandre Cançado Cardoso

 * Tested for English and Portuguese, ok for English only. To use for any other supported language (Franch and Spanish),
 * changes at the text pre-appended tag for language set is needed. (in function process(..) )
 */

/**
 * Client to the Open Calais REST API to extract DBpediaResourceOccurrences.
 * This is by no means a complete client for OpenCalais. If that's what you're looking for, try http://code.google.com/p/j-calais/
 * Our client aims at simply returning DBpediaResourceOccurrences for evaluation.
 *
 * Ease of use:
 * - Provided example Java code;
 * - Multiple APIs offered.
 * - Had two REST URLs: confusing (Also not really restful http://www.opencalais.com/forums/known-issues/rest-api-not-really-restful)
 * - Output format not really friendly to parse. There is an intermediate and a simplified format. That's not really friendly.
 * - Data returned looks more like NER than entity linking (Type-Label).
 * - Examples of things that they call entities: MusicAlbum, Product, etc. See: http://d.opencalais.com/1/type/em/e/Product.html
 * -- http://www.opencalais.com/documentation/calais-web-service-api/api-metadata/entity-index-and-definitions
 * - Links to DBpedia http://www.opencalais.com/documentation/linked-data-entities
 * - Bugs in the data.
 * -- How is Alias field created? See IBM: http://d.opencalais.com/er/company/ralg-tr1r/9e3f6c34-aa6b-3a3b-b221-a07aa7933633.html
 * -- Wrong sameAs to DBpedia
 *
 * User complaints:
 * - The Washing Post was picked up 3 times. I don't know why the terms "private" and "broker" were picked up at all. ... tagging is sometimes much better, sometimes it's like this - not very useful http://www.opencalais.com/forums/known-issues/erratic-calais-performance
 *
 * @author pablomendes (main implementation)
 * @author Alexandre Cançado Cardoso (workaround to the OpanCalais service language identification bug)
 * Last Modified: 23th/08/13
 */

public class OpenCalaisClient extends AnnotationClient {

    Log LOG = LogFactory.getLog(this.getClass());

    private static String url ="http://api.opencalais.com/tag/rs/enrich";
    // Create an instance of HttpClient.
    HttpClient client = new HttpClient();

    String apikey;

    String id = "id";
    String submitter = "dbpa";
//    String outputFormat = "Text/Simple";
    String outputFormat = "application/json";
//    String outputFormat = "XML/RDF";
    String paramsXml = "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\"\n" +
            "              xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
            "      <c:processingDirectives\n" +
            "        c:contentType=\"TEXT/RAW\"\n" +
            "        c:outputFormat=\""+outputFormat+"\"\n" +
            "        c:calculateRelevanceScore=\"true\"\n" +
            "        c:enableMetadataType=\"SocialTags\"\n" +
            "        c:docRDFaccessible=\"false\"\n" +
            "        c:omitOutputtingOriginalText=\"true\"\n" +
            "        ></c:processingDirectives>\n" +
            "      <c:userDirectives\n" +
            "        c:allowDistribution=\"false\"\n" +
            "        c:allowSearch=\"false\"\n" +
            "        c:externalID=\""+ id +"\"\n" +
            "        c:submitter=\""+submitter+"\"\n" +
            "        ></c:userDirectives>\n" +
            "      <c:externalMetadata></c:externalMetadata>\n" +
            "    </c:params>";

    public OpenCalaisClient(String apikey) {
        this.apikey = apikey;
    }

    private String dereference(String uri) throws AnnotationException {
        LOG.debug("Dereferencing: "+uri);
        GetMethod method = new GetMethod(uri);
        method.setRequestHeader("Accept", "rdf/xml");
        String response = request(method);

        //HACK I'm really sorry if you're reading this. hahaha this is UGLY!
        String[] hackSameAs = response.split("<owl:sameAs rdf:resource=\"http://dbpedia.org/resource/");
        if (hackSameAs.length>1) {
            String dbpediaURI = hackSameAs[1].substring(0, hackSameAs[1].indexOf("\"/>"));
            if (dbpediaURI.length()>1) // "http://"
                return dbpediaURI;
        }

        String[] hackRedirection = response.split("<cld:redirection rdf:resource=\"");
        if (hackRedirection.length>1) {
            String redirect = hackRedirection[1].substring(0, hackRedirection[1].indexOf("\"/>"));
            if (redirect.length()>7) // "http://"
                return dereference(redirect);
        }

        String[] hackLabel = response.split("<c:name>");
        if (hackLabel.length>1) {
            String label = hackLabel[1].substring(0, hackLabel[1].indexOf("</c:name>"));
            if (label.length()>1) // "http://"
                return label;
        }

        LOG.debug("... resulting in: "+uri);
        return uri;
    }

    private List<DBpediaResource> parseJson(Text rawText, String annotatedText) throws AnnotationException {
        List<DBpediaResource> entities = new ArrayList<DBpediaResource>();
        JSONObject jsonObj = JSONObject.fromObject(annotatedText);
        //System.out.println();
        Set entries = jsonObj.entrySet();
        for (Object o: entries) {
            Map.Entry m = (Map.Entry) o;
            String key = (String) m.getKey();
            if (key.equals("doc")) continue;

            Object bean = net.sf.json.JSONObject.toBean((JSONObject) m.getValue());

            try {
                //System.out.println(bean);
                Object entryType = PropertyUtils.getProperty(bean,"_typeGroup");

                if (entryType.equals("entities")) {

                    String uri = key;
                    Object entryName = PropertyUtils.getProperty(bean,"name");
                    String type = (String) PropertyUtils.getProperty(bean,"_type");
                    Double relevance = (Double) PropertyUtils.getProperty(bean,"relevance");

                    List instances = (List) PropertyUtils.getProperty( bean, "instances" );
                    for (Object i: instances) {
                        Integer offset = (Integer) PropertyUtils.getProperty(i,"offset");
                        String dbpediaUri = dereference(uri);
                        DBpediaResource resource = new DBpediaResource(dbpediaUri);
                        //System.out.println(response);
                        entities.add(resource);

                        //TODO For annotations we can get occurrences instead of just DBpediaResources
//                        List<DBpediaType> types = new ArrayList<DBpediaType>();
//                        types.add(new DBpediaType(type));
//                        resource.setTypes(types);
//                        DBpediaResourceOccurrence occ = new DBpediaResourceOccurrence(uri, resource, new SurfaceForm("name"), rawText, offset, Provenance.Annotation(), relevance, 1.0);
//                        System.out.printf("key: %s, name: %s, offset: %s\n", key, entryName, offset);

                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InvocationTargetException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (NoSuchMethodException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                System.out.println(o);
            }

        }

        //System.out.println(json);
        return entities;
    }

    public List<DBpediaResource> extract(Text text) throws AnnotationException{
        List<DBpediaResource> entities = parseJson(text, process(text.text()));
        return entities;
    }
    
    protected String process(String text) throws AnnotationException {
        //Pre-append English tag to text. It's a workaround to allow the text to have a word that is the name of an unsuported language. Reference:
        text = "Prefix to circumvent OpenCalais bug, this is English text" + text;
        //Original process method
        PostMethod method = new PostMethod(url);
        // Set mandatory parameters
        method.setRequestHeader("x-calais-licenseID", apikey);
        // Set input content type
        method.setRequestHeader("Content-type","application/x-www-form-urlencoded");
        // Set response/output format
        method.setRequestHeader("Accept", "application/json");
        // Define params for the body
        NameValuePair[] params = {new NameValuePair("licenseID",apikey), new NameValuePair("content",text), new NameValuePair("paramsXML",paramsXml)};
        method.setRequestBody(params);
        String response = request(method);
        //System.out.println(response);
        return response;
    }


    public static void main(String[] args) {
        String apikey = args[0];

//        OpenCalaisClient client = new OpenCalaisClient(apikey);
//        Text text = new Text("Worries over Internet privacy have spurred lawsuits, conspiracy theories and consumer anxiety as marketers and \n" +
//                "others invent new ways to track computer users on the Internet. But the alarmists have not seen anything yet. \n" +
//                "In the next few years, a powerful new suite of capabilities will become available to Web developers that could \n" +
//                "give marketers and advertisers access to many more details about computer users' online activities. Nearly \n" +
//                "everyone who uses the Internet will face the privacy risks that come with those capabilities, which are an \n" +
//                "integral part of the Web language that will soon power the Internet: HTML 5. The new Web code, the fifth \n" +
//                "version of Hypertext Markup Language used to create Web pages, is already in limited use, and it promises to \n" +
//                "usher in a new era of Internet browsing within the next few years. It will make it easier for users to view \n" +
//                "multimedia content without downloading extra software; check e-mail offline; or find a favorite restaurant or \n" +
//                "shop on a smartphone.");


//        Text text2 = new Text("I went to Germany to talk to Barack Obama at IBM.");
        
        //String json = "{\"doc\":{\"info\":{\"allowDistribution\":\"false\",\"allowSearch\":\"false\",\"calaisRequestID\":\"dbeff887-ba53-5d23-12be-3e562b236c5f\",\"externalID\":\"id\",\"id\":\"http://id.opencalais.com/ri3DvOkcv8tAy0D0dPkXWw\",\"docId\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f\",\"document\":\"\",\"docTitle\":\"\",\"docDate\":\"2010-10-25 09:58:02.796\",\"externalMetadata\":\"\",\"submitter\":\"dbpa\"},\"meta\":{\"contentType\":\"TEXT/RAW\",\"emVer\":\"7.1.1103.5\",\"langIdVer\":\"DefaultLangId\",\"processingVer\":\"CalaisJob01\",\"submitionDate\":\"2010-10-25 09:58:02.562\",\"submitterCode\":\"39637f0d-cab7-e7f0-b9e8-c3ed974c985d\",\"signature\":\"digestalg-1|DLhOuBRCxrJmSgGiG4DCVQW6zsw=|gsXhrUGy/OXuIXn1ErScnJZMU9BcEkPXts7pQHkKucN6XYqz84mrRA==\",\"language\":\"English\",\"messages\":[]}},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/cat/1\":{\"_typeGroup\":\"topics\",\"category\":\"http://d.opencalais.com/cat/Calais/TechnologyInternet\",\"classifierName\":\"Calais\",\"categoryName\":\"Technology_Internet\",\"score\":1},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/cat/2\":{\"_typeGroup\":\"topics\",\"category\":\"http://d.opencalais.com/cat/Calais/BusinessFinance\",\"classifierName\":\"Calais\",\"categoryName\":\"Business_Finance\",\"score\":0.64},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/1\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/1\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/c84b1dc7-3aa6-3ead-8c8b-904460b42c2f\",\"name\":\"Scientific revolution\",\"importance\":\"1\"},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/2\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/2\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/4f9a3d55-33f5-3738-a2f7-3e9065a5a169\",\"name\":\"Computing\",\"importance\":\"1\"},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/3\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/3\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/4025a48f-ae10-3f5a-bcd7-e576b0787d03\",\"name\":\"Hypertext\",\"importance\":\"1\"},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/4\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/4\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/5c31e8ed-0aa4-3e81-9cf7-ac9bd6f71b59\",\"name\":\"Human-computer interaction\",\"importance\":\"2\"},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/5\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/5\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/a9234a67-1d5c-3451-83e0-fb48ce4187d8\",\"name\":\"World Wide Web\",\"importance\":\"2\"},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/6\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/6\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/3f32ae31-f0e1-3522-a388-69fcd5835292\",\"name\":\"Internet\",\"importance\":\"2\"},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/7\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/7\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/c8372098-cb83-3f4f-a0a7-d2f29778bc39\",\"name\":\"Mass media\",\"importance\":\"2\"},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/8\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/8\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/046d89d7-95f1-30bc-a283-1a5ecd0c0007\",\"name\":\"Telecommunications\",\"importance\":\"2\"},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/9\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/9\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/4cdc0d4a-ba75-3ec4-8ed5-5bff7f1cfef5\",\"name\":\"Smartphone\",\"importance\":\"2\"},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/10\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/10\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/9a43d517-9eb2-377f-86fe-ebe665ef5477\",\"name\":\"Technology_Internet\",\"importance\":\"1\"},\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/11\":{\"_typeGroup\":\"socialTag\",\"id\":\"http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f/SocialTag/11\",\"socialTag\":\"http://d.opencalais.com/genericHasher-1/c1bf4420-4b56-3ef2-a429-06a6098bf2ef\",\"name\":\"Business_Finance\",\"importance\":\"2\"},\"http://d.opencalais.com/genericHasher-1/4c478bb9-1128-302d-988a-c90e8294029a\":{\"_typeGroup\":\"entities\",\"_type\":\"Technology\",\"name\":\"HTML\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/Technology\",\"instances\":[{\"detection\":\"[Web language that will soon power the Internet: ]HTML[ 5. The new Web code, the fifth \\nversion of]\",\"prefix\":\"Web language that will soon power the Internet: \",\"exact\":\"HTML\",\"suffix\":\" 5. The new Web code, the fifth \\nversion of\",\"offset\":620,\"length\":4}],\"relevance\":0.258},\"http://d.opencalais.com/genericHasher-1/596854c8-3dcc-3e06-9bae-6fa43c09782f\":{\"_typeGroup\":\"entities\",\"_type\":\"IndustryTerm\",\"name\":\"online activities\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/IndustryTerm\",\"instances\":[{\"detection\":\"[to many more details about computer users' ]online activities[. Nearly \\neveryone who uses the Internet will]\",\"prefix\":\"to many more details about computer users' \",\"exact\":\"online activities\",\"suffix\":\". Nearly \\neveryone who uses the Internet will\",\"offset\":416,\"length\":17}],\"relevance\":0.33},\"http://d.opencalais.com/genericHasher-1/d2449cb6-4f7c-315f-8cba-14ebbe444255\":{\"_typeGroup\":\"entities\",\"_type\":\"IndustryTerm\",\"name\":\"Internet privacy\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/IndustryTerm\",\"instances\":[{\"detection\":\"[Worries over ]Internet privacy[ have spurred lawsuits, conspiracy theories and]\",\"prefix\":\"Worries over \",\"exact\":\"Internet privacy\",\"suffix\":\" have spurred lawsuits, conspiracy theories and\",\"offset\":13,\"length\":16}],\"relevance\":0.36},\"http://d.opencalais.com/genericHasher-1/79b8af23-8e60-3170-b9b6-0327d40e3f9b\":{\"_typeGroup\":\"entities\",\"_type\":\"IndustryTerm\",\"name\":\"Web language\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/IndustryTerm\",\"instances\":[{\"detection\":\"[ capabilities, which are an \\nintegral part of the ]Web language[ that will soon power the Internet: HTML 5. The]\",\"prefix\":\" capabilities, which are an \\nintegral part of the \",\"exact\":\"Web language\",\"suffix\":\" that will soon power the Internet: HTML 5. The\",\"offset\":572,\"length\":12}],\"relevance\":0.258},\"http://d.opencalais.com/genericHasher-1/eba7a0d7-4d38-3454-b5c0-74d0ff18940a\":{\"_typeGroup\":\"entities\",\"_type\":\"ProgrammingLanguage\",\"name\":\"HTML\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/ProgrammingLanguage\",\"instances\":[{\"detection\":\"[Web language that will soon power the Internet: ]HTML[ 5. The new Web code, the fifth \\nversion of]\",\"prefix\":\"Web language that will soon power the Internet: \",\"exact\":\"HTML\",\"suffix\":\" 5. The new Web code, the fifth \\nversion of\",\"offset\":620,\"length\":4}],\"relevance\":0.258},\"http://d.opencalais.com/genericHasher-1/bf1bc77e-b157-32ce-a4ab-63e773115bb6\":{\"_typeGroup\":\"entities\",\"_type\":\"IndustryTerm\",\"name\":\"Web developers\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/IndustryTerm\",\"instances\":[{\"detection\":\"[suite of capabilities will become available to ]Web developers[ that could \\ngive marketers and advertisers]\",\"prefix\":\"suite of capabilities will become available to \",\"exact\":\"Web developers\",\"suffix\":\" that could \\ngive marketers and advertisers\",\"offset\":308,\"length\":14}],\"relevance\":0.33},\"http://d.opencalais.com/genericHasher-1/a4313a37-b29c-32a3-87ac-8103732e79ce\":{\"_typeGroup\":\"entities\",\"_type\":\"Technology\",\"name\":\"Hypertext Markup Language\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/Technology\",\"instances\":[{\"detection\":\"[HTML 5. The new Web code, the fifth \\nversion of ]Hypertext Markup Language[ used to create Web pages, is already in limited]\",\"prefix\":\"HTML 5. The new Web code, the fifth \\nversion of \",\"exact\":\"Hypertext Markup Language\",\"suffix\":\" used to create Web pages, is already in limited\",\"offset\":668,\"length\":25}],\"relevance\":0.18},\"http://d.opencalais.com/genericHasher-1/654a3983-0d2a-326d-ba56-efbef71e2981\":{\"_typeGroup\":\"entities\",\"_type\":\"IndustryTerm\",\"name\":\"extra software\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/IndustryTerm\",\"instances\":[{\"detection\":\"[to view \\nmultimedia content without downloading ]extra software[; check e-mail offline; or find a favorite]\",\"prefix\":\"to view \\nmultimedia content without downloading \",\"exact\":\"extra software\",\"suffix\":\"; check e-mail offline; or find a favorite\",\"offset\":915,\"length\":14}],\"relevance\":0.123},\"http://d.opencalais.com/genericHasher-1/c2742ea7-b237-30d6-8e86-6eb88c3ac6b4\":{\"_typeGroup\":\"entities\",\"_type\":\"Technology\",\"name\":\"smartphone\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/Technology\",\"instances\":[{\"detection\":\"[or find a favorite restaurant or \\nshop on a ]smartphone[.]\",\"prefix\":\"or find a favorite restaurant or \\nshop on a \",\"exact\":\"smartphone\",\"suffix\":\".\",\"offset\":997,\"length\":10}],\"relevance\":0.123},\"http://d.opencalais.com/genericHasher-1/0c61f5e6-dc3f-3ec5-8b09-ca6654971b76\":{\"_typeGroup\":\"entities\",\"_type\":\"IndustryTerm\",\"name\":\"Internet browsing\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/IndustryTerm\",\"instances\":[{\"detection\":\"[use, and it promises to \\nusher in a new era of ]Internet browsing[ within the next few years. It will make it]\",\"prefix\":\"use, and it promises to \\nusher in a new era of \",\"exact\":\"Internet browsing\",\"suffix\":\" within the next few years. It will make it\",\"offset\":789,\"length\":17}],\"relevance\":0.18},\"http://d.opencalais.com/genericHasher-1/88958341-5ef0-33a2-9f74-3ff62c50df5a\":{\"_typeGroup\":\"entities\",\"_type\":\"IndustryTerm\",\"name\":\"Web code\",\"_typeReference\":\"http://s.opencalais.com/1/type/em/e/IndustryTerm\",\"instances\":[{\"detection\":\"[that will soon power the Internet: HTML 5. The ]new Web code[, the fifth \\nversion of Hypertext Markup Language]\",\"prefix\":\"that will soon power the Internet: HTML 5. The \",\"exact\":\"new Web code\",\"suffix\":\", the fifth \\nversion of Hypertext Markup Language\",\"offset\":632,\"length\":12}],\"relevance\":0.18}}";
//        String xml = "<OpenCalaisSimple><Description><allowDistribution>false</allowDistribution><allowSearch>false</allowSearch><calaisRequestID>8912dfc8-d3cb-1e67-12bf-873471f77ac3</calaisRequestID><externalID>id</externalID><id>http://id.opencalais.com/ri3DvOkcv8tAy0D0dPkXWw</id><about>http://d.opencalais.com/dochash-1/db3b6fe9-b523-3184-9be8-33706e01947f</about><docTitle/><docDate>2010-10-29 10:45:26.933</docDate><externalMetadata/><submitter>dbpa</submitter></Description><CalaisSimpleOutputFormat><IndustryTerm count=\"1\" relevance=\"0.330\">online activities</IndustryTerm><IndustryTerm count=\"1\" relevance=\"0.360\">Internet privacy</IndustryTerm><IndustryTerm count=\"1\" relevance=\"0.258\">Web language</IndustryTerm><IndustryTerm count=\"1\" relevance=\"0.330\">Web developers</IndustryTerm><IndustryTerm count=\"1\" relevance=\"0.123\">extra software</IndustryTerm><IndustryTerm count=\"1\" relevance=\"0.180\">Internet browsing</IndustryTerm><IndustryTerm count=\"1\" relevance=\"0.180\">Web code</IndustryTerm><ProgrammingLanguage count=\"1\" relevance=\"0.258\">HTML</ProgrammingLanguage><Technology count=\"1\" relevance=\"0.258\">HTML</Technology><Technology count=\"1\" relevance=\"0.180\">Hypertext Markup Language</Technology><Technology count=\"1\" relevance=\"0.123\">smartphone</Technology><SocialTags><SocialTag importance=\"2\">Human-computer interaction</SocialTag><SocialTag importance=\"2\">World Wide Web</SocialTag><SocialTag importance=\"2\">Internet</SocialTag><SocialTag importance=\"2\">Mass media</SocialTag><SocialTag importance=\"2\">Telecommunications</SocialTag><SocialTag importance=\"2\">Smartphone</SocialTag><SocialTag importance=\"2\">Business_Finance</SocialTag><SocialTag importance=\"1\">Scientific revolution</SocialTag><SocialTag importance=\"1\">Computing</SocialTag><SocialTag importance=\"1\">Hypertext</SocialTag><SocialTag importance=\"1\">Technology_Internet</SocialTag></SocialTags><Topics><Topic Taxonomy=\"Calais\" Score=\"1.000\">Technology_Internet</Topic><Topic Taxonomy=\"Calais\" Score=\"0.640\">Business_Finance</Topic></Topics></CalaisSimpleOutputFormat></OpenCalaisSimple>";

        //System.out.println(client.extract(text));
        //System.out.println(client.parseJson(text,json));

//        client.extract(text2);
        //client.dereference("http://d.opencalais.com/er/company/ralg-tr1r/9e3f6c34-aa6b-3a3b-b221-a07aa7933633");


//        String baseDir = "C:\\cygwin\\home\\PabloMendes\\DBpediaSpotlight\\";
//        File outputFile = new File(baseDir+"AnnotationText-OpenCalais.txt.list");
//        File inputFile = new File(baseDir+"AnnotationText.txt");

//        File inputFile = new File("/home/pablo/eval/manual/AnnotationText.txt");
//        File outputFile = new File("/home/pablo/eval/manual/OpenCalais.txt");

//        File inputFile = new File("/home/pablo/eval/cucerzan/cucerzan.txt");
//        File outputFile = new File("/home/pablo/eval/cucerzan/systems/cucerzan-OpenCalais.txt");

//        File inputFile = new File("/home/pablo/eval/wikify/gold/WikifyAllInOne.txt");
//        File outputFile = new File("/home/pablo/eval/wikify/systems/OpenCalais.list");

//        File inputFile = new File("/home/pablo/eval/csaw/gold/paragraphs.txt");
//        File outputFile = new File("/home/pablo/eval/csaw/systems/OpenCalais.list");

        File inputFile = new File("/home/alexandre/Projects/Test_Files/Germany.txt");
        File outputFile = new File("/home/alexandre/Projects/Test_Files/OpenCalais-java_Germany.list");

        try {
            OpenCalaisClient client = new OpenCalaisClient(apikey);
            client.evaluate(inputFile, outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


        public static class OldRest extends OpenCalaisClient {

        private static String url ="http://api.opencalais.com/enlighten/rest";

//    String paramsXml = "<c:params xmlns:c=http://s.opencalais.com/1/pred/ \n" +
//            "        xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
//            "    <c:processingDirectives \n" +
//            "        c:contentType=\"text/txt\" \n" +
//            "        c:outputFormat=\""+outputFormat+"\">\n" +
//            "    </c:processingDirectives>\n" +
//            "    <c:userDirectives />\n" +
//            "    <c:externalMetadata />\n" +
//            "        </c:params>";

        public OldRest(String apikey) {
            super(apikey);
        }

        private PostMethod createPostMethod() {

            PostMethod method = new PostMethod(url);

            // Set mandatory parameters
            method.setRequestHeader("x-calais-licenseID", apikey);

            // Set input content type
            method.setRequestHeader("Content-type","application/x-www-form-urlencoded");
            //method.setRequestHeader("Content-Type", "text/xml; charset=UTF-8");
            //method.setRequestHeader("Content-Type", "text/html; charset=UTF-8");
            //method.setRequestHeader("Content-Type", "text/raw; charset=UTF-8");

            // Set response/output format
            //method.setRequestHeader("Accept", "xml/rdf");
            method.setRequestHeader("Accept", "application/json");

            // Enable Social Tags processing
            //method.setRequestHeader("enableMetadataType", "SocialTags");
            //method.setRequestHeader("enableMetadataType", "Entities");

            return method;
        }

        protected String process(String text) throws AnnotationException {
            PostMethod method = createPostMethod();
            NameValuePair[] params = {new NameValuePair("licenseID",apikey), new NameValuePair("content",text), new NameValuePair("paramsXML",paramsXml)};
            method.setRequestBody(params);
            String response = request(method);
            //System.out.println(response);
            return response;
        }
    }
}