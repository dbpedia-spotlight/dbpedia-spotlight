package org.dbpedia.spotlight.string;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

import java.io.*;

/**
 * Converts our disambiguation results log into ARFF format for training classifiers with Weka
 *
 * @author pablomendes
 */
public class CSV2Arff {

    /**
     * Extends Weka's CSVLoader patched by Pablo in order to accept values with hashes
     *
     */
    public static class MyCSVLoader extends CSVLoader {

        protected void initTokenizer(StreamTokenizer tokenizer) {
            tokenizer.resetSyntax();
            tokenizer.whitespaceChars(0, (' '-1));
            tokenizer.wordChars(' ','\u00FF');
            tokenizer.whitespaceChars('\t','\t');
            //tokenizer.commentChar('#'); //\uD880
            //tokenizer.quoteChar('"');
            //tokenizer.quoteChar('\'');
            tokenizer.eolIsSignificant(true);
        }

    }

    public static Instances load(String csvFile) throws IOException {
        // load CSV
        System.out.println("Loading CSV dataset");
        CSVLoader loader = new MyCSVLoader();
        loader.setFieldSeparator("\\t");
        loader.setSource(new File(csvFile));
        Instances data = loader.getDataSet();
        return data;
    }

    public static void toArff(Instances data, String arffFileName) throws IOException {
        System.out.println("Saving dataset as ARFF");
        // save ARFF
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(arffFileName));
        //saver.setDestination(new File(args[1]));
        saver.writeBatch();
    }

    public static void convert(String csvFileName, String arffFileName) throws IOException {
        toArff(load(csvFileName), arffFileName);
    }

    /**
     * takes 2 arguments:
     * - CSV input file
     * - ARFF output file
     *
     * Example:
     * /home/pablo/data/DisambiguationLog/20110415/spotlight-training.tsv /home/pablo/data/DisambiguationLog/20110415/spotlight-training.arff
     */
    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("\nUsage: CSV2Arff <input.csv> <output.arff>\n");
            System.exit(1);
        }
        String csvFile = args[0];
        String arffFileName = args[1];

        //CSV2Arff converter = new CSV2Arff();
        Instances data = load(csvFile);

        toArff(data, arffFileName);
    }
}