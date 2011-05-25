package org.dbpedia.spotlight.candidate.cooccurrence.features.training.lingpipe;

import com.aliasi.crf.ChainCrfChunker;
import com.aliasi.util.AbstractExternalizable;

import java.io.File;
import java.io.IOException;

/**
 * @author Joachim Daiber
 */
public class SimpleChunkerTest {

	public static void main(String[] args) throws ClassNotFoundException, IOException {


		ChainCrfChunker compiledCrfChunker
				= (ChainCrfChunker) AbstractExternalizable.readObject(new File("/Users/jodaiber/Desktop/chunker.model"));

		System.out.println(compiledCrfChunker.chunk("The stars are in a state of denial."));

	}

}
