package es.yrbcn.graph.weighted;

/*		 
 * Copyright (C) 2007 Paolo Boldi and Sebastiano Vigna 
 *  Modified by Carlos Castillo
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator;
import it.unimi.dsi.webgraph.labelling.BitStreamArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.GammaCodedIntLabel;
import it.unimi.dsi.webgraph.labelling.Label;

import java.io.IOException;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

/**
 * A class exposing a list of triples as an {@link ArcLabelledImmutableGraph}.
 * The triples are interpreted as labelled arcs: the first element is the
 * source, the second element is the target, and the third element must be a
 * nonnegative integer that will be saved using a {@link GammaCodedIntLabel}.
 * 
 * <p>
 * This class is mainly a useful example of how to expose of your data <i>via</i>
 * an {@link ArcLabelledImmutableGraph}, and it is also used to build test
 * cases, but it is not efficient or particularly refined.
 * 
 * <p>
 * A main method reads from standard input a list of TAB-separated triples and
 * writes the corresponding graph using {@link BVGraph} and
 * {@link BitStreamArcLabelledImmutableGraph}.
 */

public class DumpWeightedGraph {

	public static void main(String arg[]) throws JSAPException, IOException {
		final SimpleJSAP jsap = new SimpleJSAP(
				DumpWeightedGraph.class.getName(),
				"Reads a weighted graph and prints it as <src>TAB<dest>TAB<weight>",
				new Parameter[] { new UnflaggedOption("basename",
						JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED,
						JSAP.NOT_GREEDY,
						"The basename of the resulting arc-labelled graph."), });
		final JSAPResult jsapResult = jsap.parse(arg);
		if (jsap.messagePrinted())
			return;
		final String basename = jsapResult.getString("basename");

		BitStreamArcLabelledImmutableGraph graph = BitStreamArcLabelledImmutableGraph
				.loadOffline(basename);
		ArcLabelledNodeIterator nodeIterator = graph.nodeIterator();

		int curr;
		int d;
		int suc[];
		Label lab[];
		float weight;

		while (nodeIterator.hasNext()) {
			curr = nodeIterator.nextInt();
			d = nodeIterator.outdegree();
			suc = nodeIterator.successorArray();
			lab = nodeIterator.labelArray();
			for (int j = 0; j < d; j++) {
				weight = lab[j].getFloat();
				System.out.println(curr + "\t" + suc[j] + "\t" + weight);
			}
		}
	}
}
