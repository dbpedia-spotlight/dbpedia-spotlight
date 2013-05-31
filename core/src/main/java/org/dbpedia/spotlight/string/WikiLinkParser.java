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

import org.dbpedia.spotlight.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parses text annotated with wiki links [[uri|surface form]].
 * Used in evaluation to read manually generated annotations.
 *  
 * @author pablomendes
 */
public class WikiLinkParser {

    public final static String NoTag = "NoTag";

        /*
         Matches:
           [[Baghdad]]
           [[Iran-Iraq War|eight-year war]]
           [[Sabotage|acts of sabotage|0.4]]
          */
    public final static Pattern anyWikiLink = Pattern.compile("\\[+([^\\|\\]]+?)(\\|([^\\|\\]]+?))?(\\|([^\\|\\]]+?))?\\]+");
    public final static Pattern singleWikiLink = Pattern.compile("\\[+([^\\|\\]]+?)\\]+");


    /*
     * TODO PABLO This has problems if the markup is wrong (e.g. contains three brackets instead of two.) Would like to do this with regular expression matching instead, grabbing the URI, SF and counting the length all at once.
     */
    @Deprecated
    public static List<DBpediaResourceOccurrence> parseOld(String textWithWikiAnnotation) {
        List<DBpediaResourceOccurrence> occs = new ArrayList<DBpediaResourceOccurrence>();
        Text unMarkedUpText = new Text(eraseMarkup(textWithWikiAnnotation));
        int accumulatedRemovedCharsLength = 0;
        int i = 0;
        while (i < textWithWikiAnnotation.length()) {
            int start = textWithWikiAnnotation.indexOf("[[", i) + 2;
            if (start == 1)
                break;
            int end = textWithWikiAnnotation.indexOf("]]", start);
            if (end == -1)
                break;
            String sfResPair = textWithWikiAnnotation.substring(start, end);
            if (sfResPair.length()<3) // At least the pipe and one char at each side
                break;
            int middle = sfResPair.indexOf("|");
            if (middle == -1)
                break;

            DBpediaResource res = new DBpediaResource(sfResPair.substring(0, middle));
            SurfaceForm sf = new SurfaceForm(sfResPair.substring(middle+1));
            int offset = start - accumulatedRemovedCharsLength - 2; // the two starting brackets
            DBpediaResourceOccurrence occ = new DBpediaResourceOccurrence(res, sf, unMarkedUpText, offset, Provenance.Manual());
            occs.add(occ);
            i = end + 2; // the two closing brackets
            accumulatedRemovedCharsLength += (sfResPair.length()-sf.name().length()) + 4; // the four brackets (pipe is within sfResPair)
        }
        return occs;
    }

    public static List<DBpediaResourceOccurrence> parse(String textWithWikiAnnotation) {

        String cleanText = eraseMarkup(textWithWikiAnnotation);
        List<DBpediaResourceOccurrence> occs = new ArrayList<DBpediaResourceOccurrence>();

        Matcher matcher = anyWikiLink.matcher(textWithWikiAnnotation);

        int i=0;
        int accumulatedTrash = 0;
        while (matcher.find()) {
            i++;
            String sfName = matcher.group(3);
            String uri = matcher.group(1);
            if (sfName==null) sfName =uri;

            int sfLength = sfName.length();
            int start = matcher.start();
            int end = matcher.end();
            int offset = start - accumulatedTrash;
            accumulatedTrash += (end - start - sfLength);
            //double score = new Double(matcher.group(5)); // some training data comes with scores

            //System.err.println(cleanText.substring(offset,offset+sfName.length()));

            DBpediaResourceOccurrence occ = new DBpediaResourceOccurrence(new DBpediaResource(uri), new SurfaceForm(sfName), new Text(cleanText), offset);
            occs.add(occ);
            System.out.println(occ);
        }

        return occs;
    }

    

    public static String parseToMatrix(String textWithWikiAnnotation) {
        textWithWikiAnnotation = textWithWikiAnnotation.replace("\n"," ");
        StringBuffer buffer = new StringBuffer();
        String[] chunks = textWithWikiAnnotation.split("\\[+|\\]+");
        for (String chunk: chunks) {
            //System.out.println(chunk);
            int pipePos = chunk.indexOf("|");
            String[] chunkWords;
            String uri = "";
            if (pipePos>0) { // this chunk is annotated
                uri = chunk.substring(0,pipePos);
                chunkWords = chunk.substring(pipePos+1).split(" ");
            } else {        // this chunk has no spotlight
                uri = "NoTag";
                chunkWords = chunk.split(" ");
            }
            if (uri==null || uri.trim().equals("")) uri = "NoTag";
            DBpediaResource resource = new DBpediaResource(uri);
            for (String w: chunkWords) {
                w = w.trim();
                if (acceptToMatrix(w)) {
                    buffer.append(w+"\t"+resource.uri()+"\n");
                }
            }
        }
        return buffer.toString();
    }

    public static void appendToMatrix(String chunk, DBpediaResource resource, StringBuffer buffer) {
        String[] tokens = chunk.split("\\s+");
        if (tokens!=null & tokens.length>0) {
            for (String w: tokens) {
                if (WikiLinkParser.acceptToMatrix(w)) {
                    buffer.append(w+"\t"+resource.uri()+"\n");
                }
            }
        }
    }


    /**
     * hack to get all the annotations to look the same.
     * We reject tokens that are not meaningful to the spotlight.
     * @param w
     * @return
     */
    public static boolean acceptToMatrix(String w) {
        if (w==null) return false;
        w = w.trim();
        return (
                        !w.equals("") &&
                        !w.equals("\n") &&
                        !w.equals("\t") &&
                        !w.matches("\\W+") &&
                        !w.equals(",") &&
                        !w.equals("'") &&
                        !w.equals("'s") &&
                        !w.equals("\\'s") &&
                        !w.equals("\"") &&
                        !w.equals(".") &&
                        !w.matches("^\\.$") &&
                        !w.equals("\\)") &&
                        !w.equals(".)") &&
                        !w.startsWith("http://")
        );
    }

    public static String eraseMarkup(String textWithWikiAnnotation) {
        String cleanText = textWithWikiAnnotation.replaceAll(singleWikiLink.pattern(),"$1");
        cleanText = cleanText.replaceAll(anyWikiLink.pattern(),"$3");
        return cleanText;
    }

    public static void testParse(String[] args) {
        String t = "IF you can't say something good about someone, sit right here by me, [[Alice_Roosevelt_Longworth|Alice Roosevelt Longworth]], a self-proclaimed [[Hedonism|hedonist]], used to say. But it seems the greater pleasure comes from more temperate [[Gossip|gossip]].\n" +
                "New research finds that gossiping can be good for you as long as you have something nice to say.\n" +
                "In a presentation in [[September|September]], Jennifer Cole, a [[Social_psychology|social psychologist]], and Hannah Scrivener reported results from two related studies, both of which demonstrate that it's in one's self-interest to say So-and-so's second husband is adorable rather than \"She married that lout?\"\n" +
                "In the first study, intended to measure a person's short-term emotional reaction to gossiping, 140 men and women, primarily  [[Undergraduate_education|undergraduates]], were asked to talk about a fictional person either positively or negatively.\n" +
                "The second study, which looked into the long-term effects of gossiping on well-being, had 160 participants, mostly female [[Undergraduate_education|undergrads]], fill out [[Questionnaire|questionnaires]] about their tendency to [[Gossip|gossip]], their [[Self-esteem|self-esteem]] and their perceived social support.";
        System.out.println(t+"\n\n\n");
        System.out.println(eraseMarkup(t));

        List<DBpediaResourceOccurrence> list = parse(t);
        for (DBpediaResourceOccurrence occ: list) {
            System.out.println(occ);
            System.out.printf("offset: %s, length: %s\n", occ.textOffset(), occ.surfaceForm().name().length());
            System.out.println(occ.context().text().substring(occ.textOffset(),occ.textOffset()+occ.surfaceForm().name().length()));
            System.out.println();
        }

        List<DBpediaResourceOccurrence> list2 = new ArrayList<DBpediaResourceOccurrence>(list);

        Set<DBpediaResourceOccurrence> union = new TreeSet<DBpediaResourceOccurrence>(list);
        union.addAll(list2);

        Set<DBpediaResourceOccurrence> intersection = new TreeSet<DBpediaResourceOccurrence>(list);
        union.retainAll(list2);

        System.out.println("List1: "+list.size());
        System.out.println("List2: "+list2.size());
        System.out.println("Union: "+union.size());
        System.out.println("Intersection: "+intersection.size());
        System.out.println("Jaccard: "+intersection.size()/union.size());

        System.out.println("List1: "+list);
    }

    public static void main(String[] args) {
//        String t = "IF you can't say something good about someone, sit right here by me, [[Alice_Roosevelt_Longworth|Alice Roosevelt Longworth]], a self-proclaimed [[Hedonism|hedonist]], used to say. But it seems the greater pleasure comes from more temperate [[Gossip|gossip]].\n" +
//                "New research finds that gossiping can be good for you as long as you have something nice to say.\n" +
//                "In a presentation in [[September|September]], Jennifer Cole, a [[Social_psychology|social psychologist]], and Hannah Scrivener reported results from two related studies, both of which demonstrate that it's in one's self-interest to say So-and-so's second husband is adorable rather than \"She married that lout?\"\n" +
//                "In the first study, intended to measure a person's short-term emotional reaction to gossiping, 140 men and women, primarily  [[Undergraduate_education|undergraduates]], were asked to talk about a fictional person either positively or negatively.\n" +
//                "The second study, which looked into the long-term effects of gossiping on well-being, had 160 participants, mostly female [[Undergraduate_education|undergrads]], fill out [[Questionnaire|questionnaires]] about their tendency to [[Gossip|gossip]], their [[Self-esteem|self-esteem]] and their perceived social support.";

        String t = "MORE and more people are dying or suffering from fatal diseases such as [[Brain tumor|brain tumours]], mammarian or [[breast cancer]] and brain [[Bleeding|haemorrhage]] due to a lack of proper medical care.However, things may change as the Srisiam Hospital in [[Bangkok|Bangkok|0.6]] has now established the first ever [[Radiosurgery|Radio Surgery|1.0]] Institute in [[Thailand|Thailand|0.4]] -- (there are presently more than 100 radio surgery centres around the world).";

        System.out.println(t+"\n\n\n");
        System.out.println(eraseMarkup(t));

        parse(t);
    }
}
