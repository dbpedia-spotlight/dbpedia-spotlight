package org.dbpedia.spotlight.candidate.cooccurrence.features;

import weka.core.Attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Features that are part of the instances classified by an unigram classifier.
 *
 * @author Joachim Daiber
 */

public class AttributesUnigram {
	
	public static final List<String> BIGRAM_COUNTS_WEB = Arrays.asList(new String[] {"100k", "250k", "500k", "750k", "1m", "5m", "10m+", "50m+"});

	public static int getNominalBigramCount(long countWeb) {
		if(countWeb < 250000)
			return 0;
		else if(countWeb < 500000)
			return 1;
		else if(countWeb < 750000)
			return 2;
		else if(countWeb < 1000000)
			return 3;
		else if(countWeb < 5000000)
			return 4;
		else if(countWeb < 10000000)
			return 5;
		else if(countWeb < 50000000)
			return 6;
		else
			return 7;
	}

	public static final List<String> BIGRAM_SIGNIFICANCE_WEB = Arrays.asList(new String[] {"1-5M", "7M", "9M", "10M", "15m", "15m+"});

	public static int getNominalBigramSignificance(float significanceWeb) {
		if(significanceWeb < 5000000)
			return 0;
		else if(significanceWeb < 7000000)
			return 1;
		else if(significanceWeb < 9000000)
			return 2;
		else if(significanceWeb < 10000000)
			return 3;
		else if(significanceWeb < 15000000)
			return 4;
		else
			return 5;
	}

	public static Attribute unigram_count_corpus = new Attribute("count_corpus");
	public static Attribute unigram_count_web = new Attribute("count_web");
	public static Attribute bigram_left_significance_corpus = new Attribute("left_significance_corpus");
	public static Attribute bigram_left_count_corpus = new Attribute("left_count_corpus");
	public static Attribute bigram_left_significance_web = new Attribute("left_significance_web");
	public static Attribute bigram_left_count_web = new Attribute("left_count_web");
	public static Attribute bigram_right_significance_corpus = new Attribute("right_significance_corpus");
	public static Attribute bigram_right_count_corpus = new Attribute("right_count_corpus");
	public static Attribute bigram_right_significance_web = new Attribute("right_significance_web");
	public static Attribute bigram_right_count_web = new Attribute("right_count_web");
	public static Attribute trigram_left_count_web = new Attribute("left_trigram_count_web");
	public static Attribute trigram_right_count_web = new Attribute("right_trigram_count_web");
	public static Attribute trigram_middle_count_web = new Attribute("middle_trigram");
	public static Attribute next_to_uppercase = new Attribute("next_to_uppercase", Arrays.asList(new String[] {"not_next_to_uppercase", "next_to_uppercase"}));
	public static Attribute non_sentence_initial_uppercase = new Attribute("non_sentence_initial_uppercase", Arrays.asList(new String[] {"lowercase", "starts_with_uppercase", "all_uppercase"}));
	public static Attribute quoted = new Attribute("quoted", Arrays.asList(new String[] {"quoted", "not_quoted"}));
	public static Attribute pre_pos = new Attribute("pre_POS", Arrays.asList(new String[] {"pp$", "prep", "of", "a", "the", "adj"}));
	public static Attribute next_pos = new Attribute("next_POS", Arrays.asList(new String[] {"verb", "of", "for"}));
	public static Attribute possesive = new Attribute("possesive", Arrays.asList(new String[] {"yes"}));

	//public static List<String> nPOSTags = Arrays.asList(new String[] {"nn", "nn$", "nns", "nns$", "np", "np$", "nps", "nps$", "fw-nn", "fw-nn$", "fw-nns", "fw-nns$", "fw-np", "fw-np$", "fw-nps", "fw-nps$"});
	//public static Attribute POS = new Attribute("POS", nPOSTags);
	
	public static Attribute in_enumeration = new Attribute("in_enumeration", Arrays.asList(new String[] {"yes"}));

	public static Attribute candidate_class = new Attribute("class", Arrays.asList(new String[] {"term", "common"}));

	public static ArrayList<Attribute> attributeList = new ArrayList<Attribute>();;

	static {

		unigram_count_corpus.setWeight(0.5);
		attributeList.add(unigram_count_corpus);

		unigram_count_web.setWeight(0.5);
		attributeList.add(unigram_count_web);

		//Left neighbour:
		//attributeList.add(bigram_left_significance_corpus);
		//attributeList.add(bigram_left_count_corpus);
		attributeList.add(bigram_left_significance_web);
		//attributeList.add(bigram_left_count_web);

		//Right neighbour:
		//attributeList.add(bigram_right_significance_corpus);
		//attributeList.add(bigram_right_count_corpus);
		attributeList.add(bigram_right_significance_web);
		//attributeList.add(bigram_right_count_web);

		//Other properties:
		attributeList.add(quoted);

		trigram_left_count_web.setWeight(10);
		attributeList.add(trigram_left_count_web);

		trigram_right_count_web.setWeight(10);
		attributeList.add(trigram_right_count_web);

		trigram_middle_count_web.setWeight(10);
		attributeList.add(trigram_middle_count_web);

		attributeList.add(non_sentence_initial_uppercase);
		attributeList.add(next_to_uppercase);

		pre_pos.setWeight(2);
		attributeList.add(pre_pos);

		next_pos.setWeight(2);
		attributeList.add(next_pos);

		attributeList.add(possesive);
		//attributeList.add(POS);


		in_enumeration.setWeight(50);
		attributeList.add(in_enumeration);

		attributeList.add(candidate_class);
	}


	public static int i(Attribute attribute) {
		return attributeList.indexOf(attribute);
	}

}
