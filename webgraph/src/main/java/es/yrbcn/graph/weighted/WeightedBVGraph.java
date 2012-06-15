package es.yrbcn.graph.weighted;

/*		 
 * Copyright (C) 2007 Paolo Boldi and Sebastiano Vigna 
 *  
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

import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.AbstractLazyIntIterator;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.examples.IntegerTriplesArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableSequentialGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator;
import it.unimi.dsi.webgraph.labelling.BitStreamArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.GammaCodedIntLabel;
import it.unimi.dsi.webgraph.labelling.Label;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator.LabelledArcIterator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

/** A class exposing a list of triples as an {@link ArcLabelledImmutableGraph}. The triples are
 * interpreted as labelled arcs: the first element is the source, the second element is the target,
 * and the third element must be a nonnegative integer that will be saved using a {@link GammaCodedIntLabel}.
 * 
 * <p>This class is mainly a useful example of how to expose of your data <i>via</i> an {@link ArcLabelledImmutableGraph}, and
 * it is also used to build test cases, but it is not efficient or particularly refined.
 * 
 * <p>A main method reads from standard input a list of TAB-separated triples and writes the corresponding graph
 * using {@link BVGraph} and {@link BitStreamArcLabelledImmutableGraph}.
 */

public class WeightedBVGraph extends ArcLabelledImmutableSequentialGraph {
	/** The list of triples. */
	final private WeightedArc[] arclist;
	/** The prototype of the labels used by this class. */
	final private FixedWidthFloatLabel prototype;
	/** The number of nodes, computed at construction time by triple inspection. */
	final private int numNodes;
	
	public static final Logger LOGGER = it.unimi.dsi.Util.getLogger( WeightedBVGraph.class );
	
	/** Creates a new arc-labelled immutable graph using a specified list of triples.
	 * 
	 * <p>Note that it is impossible to specify isolated nodes with indices larger than
	 * the largest node with positive indegree or outdegree, as the number of nodes is computed
	 * by maximising over all indices in <code>triple</code>. 
	 * 
	 * @param arclist a list of triples specifying labelled arcs (see the {@linkplain IntegerTriplesArcLabelledImmutableGraph class documentation});
	 * order is not relevant, but multiple arcs are not allowed.
	 */
	public WeightedBVGraph( WeightedArc[] arclist ) {
		this.arclist = arclist;
		prototype = new FixedWidthFloatLabel("FOO");
		int maxNodeID = 0;
		for( int i = 0; i < arclist.length; i++ ) {
			maxNodeID = (int) Math.max( maxNodeID, Math.max( arclist[ i ].src, arclist[ i ].dest ) );
		}
		Arrays.sort( arclist, new Comparator<WeightedArc>() {
			public int compare( WeightedArc p, WeightedArc q ) {
				final float t =  p.src - q.src; // Compare by source
				if ( t != 0 ) return (int)t;
				return (int) (p.dest - q.dest ); // Compare by destination
			}
		} );
		
		numNodes = maxNodeID + 1;
	}
	
	@Override
	public Label prototype() {
		return prototype;
	}

	@Override
	public int numNodes() {
		return numNodes;
	}

	@Override
	public ArcLabelledNodeIterator nodeIterator( int from ) {
		throw new UnsupportedOperationException();
	}

	private final class ArcIterator extends AbstractLazyIntIterator implements LabelledArcIterator  {
		private final int d;
		private int k = 0; // Index of the last returned triple is pos+k
		private final int pos;
		private final FixedWidthFloatLabel label;

		private ArcIterator( int d, int pos, FixedWidthFloatLabel label ) {
			this.d = d;
			this.pos = pos;
			this.label = label;
		}

		public Label label() {
			if ( k == 0 ) throw new IllegalStateException();
			label.value = arclist[ pos + k ].weight;
			return label;
		}

		public int nextInt() {
			if ( k >= d ) return -1;
			return (int)(arclist[ pos + ++k ].dest);
		}
	}

	@Override
	public ArcLabelledNodeIterator nodeIterator() {
		return new ArcLabelledNodeIterator() {
			/** Last node returned by this iterator. */
			private int last = -1;
			/** Last triple examined by this iterator. */
			private int pos = -1;
			/** A local copy of the prototye. */
			private FixedWidthFloatLabel label = prototype.copy();

			@Override
			public LabelledArcIterator successors() {
				if ( last < 0 ) throw new IllegalStateException();
				final int d = outdegree(); // Triples to be returned are pos+1,pos+2,...,pos+d 
				return new ArcIterator( d, pos, label );
			}

			@Override
			public int outdegree() {
				if ( last < 0 ) throw new IllegalStateException();
				int p;
				for ( p = pos + 1; p < arclist.length && arclist[ p ].src == last; p++ ) {
					// Nothing
				}
				return p - pos - 1;
			}

			public boolean hasNext() {
				return last < numNodes - 1;
			}
			
			@Override
			public int nextInt() {
				if ( !hasNext() ) throw new NoSuchElementException();
				if ( last >= 0 ) pos += outdegree();
				return ++last;
			}
			
		};
	}
	
	public static void main( String arg[] ) throws JSAPException, IOException {
		final SimpleJSAP jsap = new SimpleJSAP( WeightedBVGraph.class.getName(), 
				"Reads from standard input a list of triples <source,dest,weight>, where the three " +
				"components are separated by a TAB, and saves the " +
				"corresponding arc-labelled graph using a BVGraph and a BitStreamArcLabelledImmutableGraph. " +
				"Labels are represeted using GammaCodedIntLabel.",
				new Parameter[] {
						//new FlaggedOption( "graphClass", GraphClassParser.getParser(), null, JSAP.NOT_REQUIRED, 'g', "graph-class", "Forces a Java class for the source graph." ),
						new FlaggedOption( "input", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'i', "input",  "The input file."),
						new UnflaggedOption( "basename", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The basename of the resulting arc-labelled graph." ),
					}		
				);
		
		final JSAPResult jsapResult = jsap.parse( arg );
		if ( jsap.messagePrinted() ) return;
		final String basename = jsapResult.getString( "basename" );

		// Count lines in input file
		int narcs = 0;
		System.err.println( "Counting lines of input file" );
		BufferedReader br = new BufferedReader( new InputStreamReader( new FileInputStream( jsapResult.getString( "input" ) ) , "ASCII" ), 10*1024*1024 );
		ProgressLogger pl = new ProgressLogger( LOGGER, ProgressLogger.TEN_SECONDS, "lines" );
		pl.start();
		while( br.readLine() != null ) {
			narcs++;
			pl.update();
		}
		pl.stop();
		System.err.println( "There are " + narcs + " lines, requesting memory" );
		WeightedArc[] list = new WeightedArc[narcs];
		
		// We read triples from stdin, parse them and feed them to the constructor.
		System.err.println( "Reading input file in main memory" );
		br = new BufferedReader( new InputStreamReader( new FileInputStream( jsapResult.getString( "input" ) ) , "ASCII" ), 10*1024*1024 );
				
		pl = new ProgressLogger( LOGGER, ProgressLogger.TEN_SECONDS, "arcs" );
		pl.expectedUpdates = narcs;
		pl.start();
		String line;
		int lineno = 0;
		while( ( line = br.readLine() ) != null ) {
			try {
				list[lineno] = new WeightedArc( line );
				pl.update();
			} catch( NumberFormatException e ) {
				System.err.println( e );
				System.err.println( "Remember that the input format is one line per edge, containing a triple of tab-separated values (src<tab>dest<tab>weight).");
				return;
			}
			lineno++;
		}
		pl.stop();
		final ArcLabelledImmutableGraph g = new WeightedBVGraph( list );
		
		System.err.println( "Compressing graph" );
		BVGraph.store( g, basename + ArcLabelledImmutableGraph.UNDERLYINGGRAPH_SUFFIX, new ProgressLogger( LOGGER, ProgressLogger.TEN_SECONDS, "nodes") );
		
		System.err.println( "Storing labels" );
		BitStreamArcLabelledImmutableGraph.store( g, basename, basename + ArcLabelledImmutableGraph.UNDERLYINGGRAPH_SUFFIX, new ProgressLogger( LOGGER, ProgressLogger.TEN_SECONDS, "nodes") );
		
		System.err.println( "Graph generated");
	}
}
