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

package org.dbpedia.spotlight.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.FSDirectory;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.lucene.search.LuceneCandidateSearcher;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: PabloMendes
 * Date: Jul 23, 2010
 * Time: 3:53:58 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DatasetSplitter {

    Log LOG = LogFactory.getLog(this.getClass());
    int incrementalId = 0;

    Writer mTrainingSetWriter;
    Writer mTestSetWriter;

    /**
     * Abstract constructor. Please see @link{BySize} and @link{BySurfaceForm}}
     * @param trainingSetFile
     * @param testSetFile
     * @throws IOException
     */
    public DatasetSplitter(File trainingSetFile, File testSetFile) throws IOException {
        this.mTrainingSetWriter = new BufferedWriter(new FileWriter(trainingSetFile));
        this.mTestSetWriter = new BufferedWriter(new FileWriter(testSetFile));
    }

    public abstract boolean shouldKeepTheseOccurrences(List<String> items);

    public abstract void split(List<String> items) throws IOException;

    //TODO Max: question: does this assume sorting by URI?
    public void run(InputStream stream) throws IOException {
        String currentItem = "";
        List<String> items = new ArrayList<String>();
        Scanner scanner = new Scanner(new InputStreamReader(stream, "UTF-8"));
        int nItemsKept = 0;
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            incrementalId++;

            if (line==null || line.trim().equals(""))
                continue;

            String[] fields = line.split("\t");
            String uri;
            if (fields.length >= 5) {
                uri = fields[0];
            }
            else {
                uri = fields[1];
            }
//                    String surfaceForm = fields[1];
//                    String context = fields[2];
//                    String offset = fields[3];
//                    String type = fields[4];

            //Tuple5<String,String,String,String,String> t = new Tuple5<String,String,String,String,String>(surfaceForm, uri, context, offset, type);

            if ( !uri.equals(currentItem)){

                if (shouldKeepTheseOccurrences(items)) {
                    nItemsKept++;
                    LOG.trace("End of current item: "+currentItem+" / size: "+items.size()+" - saving!");
                    split(items);
                } // else ignore
                //reset current item
                currentItem = uri;
                items = new ArrayList<String>();
            }
            items.add(line.toString());

            if (incrementalId % 50000 == 0)
                LOG.info("Processed "+incrementalId+" occurrences. Kept occurrences for "+nItemsKept+" URIs.");
        }
        scanner.close();
        LOG.info("Processed "+incrementalId+" occurrences. Kept occurrences for "+nItemsKept+" URIs");
    }

//    public void run(File f) {
//        LOG.info("Loading occurrences from "+f.getPath());
//        String currentItem = "";
//        //Set<Tuple5> items = new HashSet<Tuple5>();
//        List<String> items = new ArrayList<String>();
//
//        if (f.getName().length() != 0) {
//            try {
//                FastBufferedReader in = new FastBufferedReader(new FileReader(f));
//                MutableString line = new MutableString();
//                int i = 0;
//                while ((line = in.readLine(line)) != null) {
//                    incrementalId++;
//
//                    if (line==null || line.trim().equals(""))
//                        continue;
//
//                    String[] fields = line.toString().split("\t");
////                    String surfaceForm = fields[0];
//                    String uri = fields[1];
////                    String context = fields[2];
////                    String offset = fields[3];
////                    String type = fields[4];
//
//                    //Tuple5<String,String,String,String,String> t = new Tuple5<String,String,String,String,String>(surfaceForm, uri, context, offset, type);
//
//                    if ( !uri.equals(currentItem)){
//                        if (i >= mMinNumberOfExamples) {
//                            uniformSplit(items);
//                        } // else ignore
//                        //reset current item
//                        currentItem = uri;
//                        items = new ArrayList<String>();
//                    }
//                    items.add(line.toString());
//                    i++;
//                }
//                in.close();
//
//            } catch (IOException e) {e.printStackTrace();}
//        }
//        LOG.info("Done. Loaded "+items.size()+" items.");
//
//    }



    public void write(int id, String item, Writer writer) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(id);
        sb.append("\t");
        sb.append(item);
        sb.append("\n");
        writer.write(sb.toString());
    }

    public static class BySize extends DatasetSplitter {

        int mMinNumberOfExamples = 1;
        double mPercentSplit = 0.5;

        public BySize(File trainingSetFile, File testSetFile, int minNumberOfExamples, double percentSplit) throws IOException {
            super(trainingSetFile, testSetFile);
            this.mMinNumberOfExamples = minNumberOfExamples;
            this.mPercentSplit = percentSplit;
        }

        @Override
        public boolean shouldKeepTheseOccurrences(List<String> items) {
            return items.size() >= mMinNumberOfExamples;
        }

        @Override
        public void split(List<String> items) throws IOException {
            int i = incrementalId-items.size(); // set
            int n = (new Double(items.size() * mPercentSplit)).intValue();
            for (String item: items) {
                if((n>0) && // When there are enough items for dividing in training and testing
                        (i % (items.size() / n) == 0)){  // For a 10% split, uniformSplit every 10th entry
                    LOG.trace("Writing to test: "+i+" "+items.size()+"/"+ n );
                    write(i, item, mTestSetWriter);
                } else {
                    // For a 10% split, it will write to training 90% of the times, plus
                    // when there are not enough examples to split between training and testing
                    // That should assure that all senses are in training to be picked.
                    LOG.trace("Writing to training: "+i);
                    write(i, item, mTrainingSetWriter);
                }

                i++;
            }
        }

    }

    public static class BySurfaceForm extends BySize {

        Set<String> mValidSurfaceForms = new HashSet<String>();

        public BySurfaceForm(File trainingSetFile, File testSetFile, int minNumberOfExamples, double percentSplit, Set<String> validSurfaceForms) throws IOException {
            super(trainingSetFile, testSetFile, minNumberOfExamples, percentSplit);
            mValidSurfaceForms = validSurfaceForms;
            LOG.info("Assuming "+validSurfaceForms.size()+" valid surface forms to acquire occurrence samples.");
        }

        @Override
        public boolean shouldKeepTheseOccurrences(List<String> items) {
            boolean shouldKeep = false;
            for (String item: items) {
                StringBuffer sf = new StringBuffer();
                try {
                    String[] fields = item.split("\t");
                    if (fields.length >= 5) {
                        sf = sf.append(fields[2]);
                    }
                    else {
                        sf = sf.append(fields[1]);
                    }
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    LOG.debug("Error parsing line: "+item);
                }
                for (String validSf: mValidSurfaceForms) {
                    //if (sf.toString().toLowerCase().contains(validSf.toLowerCase())) { // relaxed
                    if (sf.toString().toLowerCase().equals(validSf.toLowerCase())) {   // strict
                        shouldKeep = true;
                        LOG.trace("Kept:"+sf+" because it matches "+validSf);
                        break;
                    }
                }
            }
            return shouldKeep;
        }

    }


    /**
     * TODO created by Max: this functions allows for one call to create "confusable-with" sets
     * For a given type, goes through the data set that keeps the types for each resource.
     * If the type matches, look in the surrogate index for this URI (opposite direction as usually)
     * for all surface forms that can relate to this URI.
     * Return all surface forms found this way. 
     */
    public static Set<String> getConfusableSurfaceForms(String targetType, File instancesFile, LuceneCandidateSearcher surrogateSearcher) throws IOException, ParseException {
        System.err.println("Getting all surface forms for "+targetType+"s...");
        Set<String> surfaceForms = new HashSet<String>();
        if (!targetType.startsWith(SpotlightConfiguration.DEFAULT_ONTOLOGY_PREFIX))
            targetType = SpotlightConfiguration.DEFAULT_ONTOLOGY_PREFIX+ targetType;
        NxParser parser = new NxParser(new FileInputStream(instancesFile));
        while (parser.hasNext()) {
            Node[] triple = parser.next();
            if (triple[2].toString().equals(targetType)) {
                String targetUri = triple[0].toString().replace(SpotlightConfiguration.DEFAULT_NAMESPACE, "");
                try {
                    Set<SurfaceForm> surfaceFormsForURI = surrogateSearcher.getSurfaceForms(new DBpediaResource(targetUri));
                    for (SurfaceForm sf : surfaceFormsForURI) {
                        surfaceForms.add(sf.name());    
                    }
                }
                catch (SearchException e) {
                    System.err.println("URI "+targetUri+" not found in surrogate index. Skipping.");
                }
            }
        }

        return surfaceForms;
    }



    //TODO Make this guy parameterizable from command line.
    public static void main(String[] args) throws IOException, ParseException {
        /**
         * Split dataset in training and test.
         * percentageSplit indicates how much to save for testing
         * minSize indicates the minimum number of occurrences a URI has to have for it to make it to training/testing
         */
        int minSize = 2;
        double percentageSplit = 0.5;
        String targetType = "Actor"; //"Person";  //Place  //Organisation
        /*
        Here I'm using wikipediaOccurrences.ambiguous.tsv.gz
        Be careful here. Do not use withtype because it is a join with the types,
        so for URIs that have multiple types the same entry is repeated multiple times.
         */

        System.err.println("Making confusable with "+targetType+" data sets.");

        File inputFile = new File("data/WikipediaOccurrences-IDs-clean_enwiki-20100312.uriSorted.tsv");
        File trainingFile = new File("E:/dbpa/data/Person_newSurrogates/wikipediaTraining."+(new Double((1-percentageSplit)*100)).intValue()+"."+targetType+".amb.tsv");
        File testFile = new File("E:/dbpa/data/Person_newSurrogates/wikipediaTest."+(new Double(percentageSplit*100)).intValue()+"."+targetType+".amb.tsv");

        // using the next few lines, to create "confusable-with", split in training and testing
        File instancesFile = new File("data/dbpedia/instance_types_en.nt");
        File surrogateIndexDir = new File("data/SurrogateIndex.TitRedDisOcc.lowerCase");
        LuceneManager manager = new LuceneManager.CaseInsensitiveSurfaceForms(FSDirectory.open(surrogateIndexDir));
        LuceneCandidateSearcher surrogateSearcher = new LuceneCandidateSearcher(manager, false);
        Set<String> surfaceForms = getConfusableSurfaceForms(targetType, instancesFile, surrogateSearcher);

        DatasetSplitter splitter = new BySurfaceForm(trainingFile, testFile, minSize, percentageSplit, surfaceForms);
        //DatasetSplitter splitter = new BySize(trainingFile, testFile, minSize, percentageSplit);

        splitter.run(new FileInputStream(inputFile));
        //splitter.run(new GZIPInputStream(new FileInputStream(inputFile)));

    }


}
