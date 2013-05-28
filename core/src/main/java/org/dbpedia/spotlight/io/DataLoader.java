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

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: PabloMendes
 * Date: Jul 23, 2010
 * Time: 3:53:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataLoader {

    Log LOG = LogFactory.getLog(this.getClass());
    LineParser mParser;
    File f;

    public DataLoader(LineParser parser) {
        this.mParser = parser;
    }

    public DataLoader(LineParser parser, File f) {
        this.mParser = parser;
        this.f = f;
    }

    public Map<String,Double> loadPriors() {
        return loadPriors(f);
    }
    
    public Map<String,Double> loadPriors(InputStream in) throws IOException {

        Map<String,Double> items = new HashMap<String,Double>();
        Scanner scanner = new Scanner(new InputStreamReader(in, "UTF-8"));
        int i = 0;
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            try {
                    mParser.add(line.toString(), items);
                    i++;

                in.close();

            } catch (IOException e) {e.printStackTrace();}
        }
        LOG.info("Done. Loaded "+items.size()+" items.");

        return items;
    }

    public Map<String,Double> loadPriors(File f) {

        LOG.info("Loading items from "+f.getPath());
        Map<String,Double> items = new HashMap<String,Double>();

        if (f.getName().length() != 0) {
            try {
                BufferedReader in  = new BufferedReader(new FileReader(f));  //FastBufferedReader in = new FastBufferedReader(new FileReader(f));
                String line;  //MutableString line = new MutableString();
                int i = 0;
                while ((line = in.readLine()) != null) {

                    if (line==null || line.trim().equals(""))
                        continue;

                    mParser.add(line.toString(), items);
                    i++;
                }
                in.close();

            } catch (IOException e) {e.printStackTrace();}
        }
        LOG.info("Done. Loaded "+items.size()+" items.");

        return items;
    }

    abstract static class LineParser {
        Log LOG = LogFactory.getLog(this.getClass());
        public abstract void add(String line, Map<String,Double> items);
    }

    public static class TSVParser extends LineParser {
        int key = 0;
        int value = 1;
        //public TSVParser(int key, int value) { this.key = key; this.value=value; }
        @Override
        public void add(String line, Map<String,Double> items) {
            String[] elements = line.split("[\\t\\s]+");

            try {
                String uri = elements[key];
                Double prior = new Double(elements[value]);
                items.put(uri,prior);
            } catch (IndexOutOfBoundsException e) {
                LOG.error("Expecting tsv file with one (String,Double) per line. Strange line: " + line);
            }
        }
    }

    public static class PigDumpParser extends LineParser {

        Writer mWriter;
        long nOutputItems = 0;

        public PigDumpParser() { }

        public PigDumpParser(Writer writer) {
            this.mWriter = writer;
        }

        /*
   ((British India (band),1L,{(British_India_%28band%29)}),1L,{()})
   ((British India Army,2L,{(British_India_Army),(British_India_Army)}),1L,{()})
        */
        @Override
        public void add(String line, Map<String,Double> items) {
            Pattern regex = Pattern.compile("\\((.+),(\\d+)L,.*");
            Matcher matcher = regex.matcher(line.toString());
            try {
                StringBuilder sb = new StringBuilder();
                if (matcher.find()) {
                    //String uri = matcher.group(1);
                    //Double prior = new Double(matcher.group(2));
//                    LOG.trace(line);
//                    LOG.trace(uri + "\t" + prior);

                    try {

                        if (mWriter!=null) {
                            sb.append(matcher.group(1)); // uri
                            sb.append("\t");
                            sb.append(matcher.group(2)); // prior
                            sb.append("\n");
                            mWriter.write(sb.toString());
                            nOutputItems++;
                        } else {
                            items.put(matcher.group(1),new Double(matcher.group(2)));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }

                } else {
                    LOG.error("Cannot parse "+line.toString());
                }
            } catch(NumberFormatException e) {
                LOG.error("Cannot parse "+line.toString()+" >>>> "+matcher.group(0)+"    "+matcher.group(1));
                e.printStackTrace();
            } catch (IndexOutOfBoundsException e) {
                LOG.error("Expecting PigDump file with one (String,Long) per line. Strange line: " + line);
            }
        }

        public void close() throws IOException {
            if (mWriter!=null)
                mWriter.close();
            LOG.info("Done. Output "+nOutputItems+" items.");
        }
    }

    public static void main(String args[]) {
        try {
            File inputFile = new File("data/Distinct-uri-By-surfaceForm.grouped");
            PigDumpParser parser = new PigDumpParser(new BufferedWriter(new FileWriter("data/Distinct-uri-By-surfaceForm.csv")));
            DataLoader loader = new DataLoader(parser);
            loader.loadPriors(inputFile);
            parser.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
