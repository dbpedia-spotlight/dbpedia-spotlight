package org.dbpedia.spotlight.candidate.cooccurrence.features;

import weka.core.Attribute;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Features that are part of the instances classified by an ngram classifier.
 *
 * @author Joachim Daiber
 */
public class AttributesNGram {

	public static Attribute count_corpus = new Attribute("count_corpus");
	public static Attribute count_web = new Attribute("count_web");
	public static Attribute contains_verb = new Attribute("contains_verb", Arrays.asList(new String[] {"no", "vb", "vbd", "vbg", "vbn", "be", "multiple"}));
	public static Attribute term_case = new Attribute("case", Arrays.asList(new String[] {"all_lowercase", "mixed", "titlecase", "all_uppercase", "first_uppercase"}));
	public static Attribute quoted = new Attribute("quoted", Arrays.asList(new String[] {"yes"}));
	public static Attribute term_size = new Attribute("term_size");
	public static Attribute pre_pos = new Attribute("pre_pos", Arrays.asList(new String[] {"to", "verb", "a"}));
	public static Attribute next_pos = new Attribute("next_pos", Arrays.asList(new String[] {"of", "to", "be", "verb"}));
	public static Attribute ends_with = new Attribute("ends_with", Arrays.asList(new String[] {"prep"}));
	public static Attribute bigram_left = new Attribute("bigram_left", AttributesUnigram.BIGRAM_COUNTS_WEB);
	public static Attribute bigram_right = new Attribute("bigram_right", AttributesUnigram.BIGRAM_COUNTS_WEB);
	public static Attribute trigram_left = new Attribute("trigram_left");
	public static Attribute trigram_right = new Attribute("trigram_right");

	public static Attribute candidate_class = new Attribute("candidate_class", Arrays.asList(new String[] {"term", "common", "part"}));
	public static ArrayList<Attribute> attributeList = new ArrayList<Attribute>();

	static {

		//attributeList.add(count_corpus);
		//attributeList.add(count_web);
		attributeList.add(contains_verb);
		attributeList.add(term_case);		
		attributeList.add(term_size);
		attributeList.add(pre_pos);
		attributeList.add(next_pos);
		attributeList.add(ends_with);
		attributeList.add(quoted);
		attributeList.add(bigram_left);
		attributeList.add(bigram_right);
		attributeList.add(trigram_left);
		attributeList.add(trigram_right);
		attributeList.add(candidate_class);

	}

	public static int i(Attribute attribute) {
		return attributeList.indexOf(attribute);
	}

}
