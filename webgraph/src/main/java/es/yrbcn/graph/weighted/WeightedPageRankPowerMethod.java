package es.yrbcn.graph.weighted;

/*
 * Copyright (C) 2004-2007 Paolo Boldi, Massimo Santini and Sebastiano Vigna
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

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import it.unimi.dsi.law.Util;
import it.unimi.dsi.law.util.NormL1;
import it.unimi.dsi.law.util.NormL2;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.util.Properties;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator;
import it.unimi.dsi.webgraph.labelling.Label;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

// RELEASE-STATUS: DIST

/** Computes PageRank using the Power Method.
 *
 * <P>The Power Method computes the principal eigenvector of a matrix <var>M</var> starting
 * from an (arbitrary) vector <var><b>x</b></var><sup>(0)</sup> and iterating the computation
 * <div style="text-align: center">
 * <big>(</big><var><b>x</b></var><sup>(<var>t</var>+1)</sup><big>)</big>' = <big>(</big><var><b>x</b></var><sup>(<var>t</var>)</sup><big>)</big>' <var>M</var>
 * </div>
 *
 * <P>Note that
 *  <div style="text-align: center">
 * <big>(</big><var><b>x</b></var><sup>(<var>t</var>+1)</sup><big>)</big>' =
 * <big>(</big><var><b>x</b></var><sup>(<var>t</var>)</sup><big>)</big>' <var>M</var> =
 * <big>(</big><var><b>x</b></var><sup>(<var>t</var>)</sup><big>)</big>' (&alpha; <var>P</var> + &alpha; <var><b>d</b></var><var><b>u</b></var>' +  (1&minus;&alpha;)<b>1</b> <var><b>v</b></var>') =
 * &alpha;<big>(</big><var><b>x</b></var><sup>(<var>t</var>)</sup><big>)</big>' <var>P</var> + &alpha; <big>(</big><var><b>x</b></var><sup>(<var>t</var>)</sup><big>)</big>'<var><b>d</b></var> <var><b>u</b></var>' + (1&minus;&alpha;) <var><b>v</b></var>'.
 * </div>
 *
 * The latter formula means that
 * <div style="text-align: center">
 * <var>x<sub>i</sub></var><sup>(<var>t</var>+1)</sup>=
 * &alpha;<big>&Sigma;</big><sub><var>j</var> &rarr; <var>i</var></sub> <var>p<sub>ji</sub></var><var>x<sub>j</sub></var><sup>(<var>t</var>)</sup>
 * + &alpha;&kappa;<var>u</var><sub><var>i</var></sub>
 * + (1&minus;&alpha;)<var>v</var><sub><var>i</var></sub>
 * </div>
 *
 * where &kappa; is the sum of <var>x<sub>j</sub></var><sup>(<var>t</var>)</sup>
 * over all dangling nodes. This is the formula used in the code.
 *
 * <P>The attribute {@link #previousRank} represents the ranking at the previous step.
 *
 */

public class WeightedPageRankPowerMethod extends WeightedPageRank {
	private final static Logger LOGGER = it.unimi.dsi.Util.getLogger( WeightedPageRankPowerMethod.class );
	
	/** The rank vector after the last iteration (only meaningful after at least one step). */
	public double[] previousRank = null;
	/** A progress logger. */
	protected final ProgressLogger progressLogger;
	/** If not <code>null</code>, the subset of nodes over which the derivatives should be computed. */
	public int[] subset;
	/** The value of derivatives  (only for the subset of nodes specified in {@link #subset}, if not <code>null</code>). */ 
	public double[][] derivative;
	/** The order of the derivatives. Must be non-<code>null</code>, but it can be the empty array. */
	public int[] order = IntArrays.EMPTY_ARRAY;
	/** If not <code>null</code>, the basename for coefficents. */
	public String coeffBasename;

	/** Creates a new PageRank Power&ndash;Method based calculator.
	 *
	 * @param g the graph.
	 * @param logger a logger that will be passed to <code>super()</code>.
	 */
	public WeightedPageRankPowerMethod( final ArcLabelledImmutableGraph g, final Logger logger ) {
		super( g, logger );
		progressLogger = new ProgressLogger( logger, "nodes" );
	}
	
	/** Creates a new PageRank Power-Method based calculator.
	 *
	 * @param g the graph.
	 */
	public WeightedPageRankPowerMethod( final ArcLabelledImmutableGraph g ) {
		this( g, LOGGER );
	}
	
	
	public void init() throws IOException {
		super.init();
		if ( start != null && ( coeffBasename != null || order.length > 0 ) ) throw new IllegalArgumentException( "You cannot choose a preference vector when computing coefficients or derivatives" );
		// Creates the arrays, if necessary
		if ( previousRank == null ) previousRank = new double[ numNodes ];
		derivative = new double[ order.length ][ subset != null ? subset.length : g.numNodes() ];
		if ( IntArrayList.wrap( order ).indexOf( 0 ) != -1 ) throw new IllegalArgumentException( "You cannot compute the derivative of order 0 (use PageRank instead)" );
		if ( coeffBasename != null ) BinIO.storeDoubles( rank, coeffBasename + "-" + 0 );

		logger.info( "Completed." );
	}
	
	/** Computes the next step of the Power Method.
	 */
	public void step() throws IOException {
		double[] oldRank = rank, newRank = previousRank;
		DoubleArrays.fill( newRank, 0.0 );
		
		// for each node, calculate its outdegree and redistribute its rank among pointed nodes
		double accum = 0.0;
		
		progressLogger.expectedUpdates = numNodes;
		progressLogger.start( "Iteration " + ( ++iterationNumber ) + "..." );
		
		final ArcLabelledNodeIterator nodeIterator = g.nodeIterator();
		int i, outdegree, j, n = numNodes;
		int[] succ;
		Label[] lab;
		
		while( n-- != 0 ) {
			i = nodeIterator.nextInt();
			outdegree = nodeIterator.outdegree();
			
			if ( outdegree == 0 || buckets != null && buckets.get( i ) ) accum += oldRank[ i ];
			else {
				j = outdegree;
				succ = nodeIterator.successorArray();
				lab = nodeIterator.labelArray();
				while ( j-- != 0 ) {
					newRank[ succ[ j ] ] += ( oldRank[ i ] * lab[j].getFloat() ) / sumoutweight[i];
				}
			}
			progressLogger.update();
		}
		progressLogger.done();
		
		final double accumOverNumNodes = accum / numNodes;
		
		final double oneOverNumNodes = 1.0 / numNodes;
		if ( preference != null )
			if ( preferentialAdjustment == null )
				for( i = numNodes; i-- != 0; ) newRank[ i ] = alpha * newRank[ i ] + ( 1 - alpha ) * preference.getDouble( i ) + alpha * accumOverNumNodes;
			else
				for( i = numNodes; i-- != 0; ) newRank[ i ] = alpha * newRank[ i ] + ( 1 - alpha ) * preference.getDouble( i ) + alpha * accum * preferentialAdjustment.getDouble( i );
		else
			if ( preferentialAdjustment == null )
				for( i = numNodes; i-- != 0; ) newRank[ i ] = alpha * newRank[ i ] + ( 1 - alpha ) * oneOverNumNodes + alpha * accumOverNumNodes;
			else
				for( i = numNodes; i-- != 0; ) newRank[ i ] = alpha * newRank[ i ] + ( 1 - alpha ) * oneOverNumNodes + alpha * accum * preferentialAdjustment.getDouble( i );
		
		//make the rank just computed the new rank
		rank = newRank;
		previousRank = oldRank;

		// Compute derivatives.
		n = iterationNumber;

		if ( subset == null ) {
			for( i = 0; i < order.length; i++ ) {
				final int k = order[ i ];
				final double alphak = Math.pow( alpha, k );
				final double nFallingK = Util.falling( n, k );
				for( j = 0; j < numNodes; j++ ) derivative[ i ][ j ] += nFallingK * ( rank[ j ] - previousRank[ j ] ) / alphak;
			}
		}
		else {
			for( i = 0; i < order.length; i++ ) {
				final int k = order[ i ];
				final double alphak = Math.pow( alpha, k );
				final double nFallingK = Util.falling( n, k );

				for( int t: subset ) derivative[ i ][ t ] += nFallingK * ( rank[ t ] - previousRank[ t ] ) / alphak;
			}
		}
		
		// Compute coefficients, if required.

		if ( coeffBasename != null ) { 
			final DataOutputStream coefficients = new DataOutputStream( new FastBufferedOutputStream( new FileOutputStream( coeffBasename + "-" + ( iterationNumber ) ) ) );
			final double alphaN = Math.pow( alpha, n );
			for( i = 0; i < numNodes; i++ ) coefficients.writeDouble( ( rank[ i ] - previousRank[ i ] ) / alphaN );
			coefficients.close();			
		}
	}
	
	public void stepUntil( final StoppingCriterion stoppingCriterion ) throws IOException {
		super.stepUntil( stoppingCriterion );
		
		for( int i = 0; i < order.length; i++ ) {
			if ( iterationNumber < order[ i ] / ( 1 - alpha ) ) LOGGER.info( "Error bound for derivative of order " + order[ i ] + " (alpha=" + alpha + "): unknown" );
			else {
				final int k = order[ i ];
				final double delta = alpha * iterationNumber / ( iterationNumber + k );
				final double alphak = Math.pow( alpha, k );
				final double nFallingK = Util.falling( iterationNumber, k );
				double infinityNorm = 0;
				for( int j = 0; j < numNodes; j++ ) infinityNorm = Math.max( infinityNorm, nFallingK * ( rank[ j ] - previousRank[ j ] ) / alphak );

				LOGGER.info( "Error bound for derivative of order " + k + " (alpha=" + alpha + "): " + infinityNorm * delta / ( 1 - delta ) );
			}
		}
	}
	
	public double normDelta() {
		double delta = 0.0;
		switch ( norm ) {
			case L1: delta = NormL1.compute( rank, previousRank ); break;
			case L2: delta = NormL2.compute( rank, previousRank ); break;
			case INFTY:
				double d;
				for (int i = rank.length; i-- !=  0; ) {
					d = Math.abs( rank[ i ] - previousRank[ i ] );
					if ( d > delta ) delta = d;
				}
				break;
		}
		logger.debug( "Current " + norm + "-norm: " + delta );
		return delta;
	}
	
	public void clear() {
		super.clear();
		previousRank = null;
	}
	
	public static void main( final String[] arg ) throws IOException, JSAPException, ConfigurationException, ClassNotFoundException {
		
		SimpleJSAP jsap = new SimpleJSAP( WeightedPageRankPowerMethod.class.getName(), "Computes PageRank of a graph with given graphBasename using the power method."
			+ "The resulting doubles are stored in binary form in rankFile."
			+ "\n[STOPPING CRITERION] The computation is stopped as soon as two successive iterates have"
			+ "an L2-distance smaller than a given threshold (-t option); in any case no more than a fixed"
			+ "number of iterations (-i option) is performed.",
			new Parameter[] {
			new FlaggedOption( "alpha", JSAP.DOUBLE_PARSER, Double.toString( WeightedPageRank.DEFAULT_ALPHA ), JSAP.NOT_REQUIRED, 'a', "alpha", "Damping factor."),
			new FlaggedOption( "maxIter", JSAP.INTEGER_PARSER, Integer.toString( WeightedPageRank.DEFAULT_MAX_ITER ), JSAP.NOT_REQUIRED, 'i', "max-iter", "Maximum number of iterations."),
			new FlaggedOption( "threshold", JSAP.DOUBLE_PARSER, Double.toString( WeightedPageRank.DEFAULT_THRESHOLD ), JSAP.NOT_REQUIRED, 't', "threshold", "Threshold to determine whether to stop."),
			new FlaggedOption( "coeff", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'c', "coeff", "Save the k-th coefficient of the Taylor polynomial using this basename." ),
			new FlaggedOption( "derivative", JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'd', "derivative", "The order(s) of the the derivative(s) to be computed (>0)." ).setAllowMultipleDeclarations( true ),
			new FlaggedOption( "preferenceVector", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'p', "preference-vector", "A preference vector stored as a vector of binary doubles." ),
			new FlaggedOption( "preferenceObject", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'P', "preference-object", "A preference vector stored as a serialised DoubleList." ),
			new FlaggedOption( "startFilename", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, '1', "start", "Start vector filename." ),
			new FlaggedOption( "buckets", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'b', "buckets", "The buckets of the graph; if supplied, buckets will be treated as dangling nodes."),
			new Switch( "offline", 'o', "offline", "use loadOffline() to load the graph" ),
			new Switch( "strongly", 'S', "strongly", "use the preference vector to redistribute the dangling rank." ),
			new Switch( "sortedRank", 's', "sorted-ranks", "Store the ranks (from highest to lowest) into <rankBasename>-sorted.ranks."),
			new FlaggedOption( "norm", JSAP.STRING_PARSER, WeightedPageRank.Norm.INFTY.toString(), JSAP.NOT_REQUIRED, 'n', "norm", "Norm type. Possible values: " + Arrays.toString( WeightedPageRank.Norm.values() ) ),
			new UnflaggedOption( "graphBasename", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The basename of the graph." ),
			new UnflaggedOption( "rankBasename", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The filename where the resulting rank (doubles in binary form) are stored." )
		}
		);
		
		JSAPResult jsapResult = jsap.parse( arg );
		if ( jsap.messagePrinted() ) return;
		
		final boolean offline = jsapResult.getBoolean( "offline", false );
		final boolean strongly = jsapResult.getBoolean( "strongly", false );
		final boolean sorted = jsapResult.getBoolean( "sortedRank", false );
		final int[] order = jsapResult.getIntArray( "derivative" );
		final String graphBasename = jsapResult.getString( "graphBasename" );
		final String rankBasename = jsapResult.getString( "rankBasename" );
		final String buckets = jsapResult.getString( "buckets" );
		final String startFilename = jsapResult.getString( "startFilename", null );
		final String coeffBasename = jsapResult.getString( "coeff" );
		final String norm = jsapResult.getString( "norm" );
		final ProgressLogger progressLogger = new ProgressLogger( LOGGER, "nodes" );
		
		ArcLabelledImmutableGraph graph = offline? ArcLabelledImmutableGraph.loadOffline( graphBasename, progressLogger ) : ArcLabelledImmutableGraph.loadSequential( graphBasename, progressLogger );
		
		DoubleList preference = null;
		String preferenceFilename = null;
		if ( jsapResult.userSpecified( "preferenceVector" ) ) 
			preference = DoubleArrayList.wrap( BinIO.loadDoubles( preferenceFilename = jsapResult.getString( "preferenceVector" ) ) );

		if ( jsapResult.userSpecified( "preferenceObject" ) ) {
			if ( jsapResult.userSpecified( "preferenceVector" ) ) throw new IllegalArgumentException( "You cannot specify twice the preference vector" );
			preference = (DoubleList)BinIO.loadObject( preferenceFilename = jsapResult.getString( "preferenceObject" ) );
		}
		
		if ( strongly && preference == null ) throw new IllegalArgumentException( "The 'strongly' option requires a preference vector" );
		
		DoubleList start = null;
		if ( startFilename != null ) {
			LOGGER.debug( "Loading start vector \"" + startFilename +"\"...");
			start = DoubleArrayList.wrap( BinIO.loadDoubles( startFilename ) );
			LOGGER.debug( "done." );
		}
		
		WeightedPageRankPowerMethod pr = new WeightedPageRankPowerMethod( graph );
		pr.alpha = jsapResult.getDouble( "alpha" );
		pr.preference = preference;
		pr.buckets = (BitSet)( buckets == null ? null : BinIO.loadObject( buckets ) );
		pr.stronglyPreferential = strongly;
		pr.start = start;
		pr.norm = WeightedPageRank.Norm.valueOf( norm );
		pr.order = order != null ? order : null;
		pr.coeffBasename = coeffBasename;
		
		// cycle until we reach maxIter interations or the norm is less than the given threshold (whichever comes first)
		pr.stepUntil( or( new WeightedPageRank.NormDeltaStoppingCriterion( jsapResult.getDouble( "threshold" ) ), new WeightedPageRank.IterationNumberStoppingCriterion( jsapResult.getInt( "maxIter" ) ) ) );
		
		System.err.print( "Saving ranks..." );
		BinIO.storeDoubles( pr.rank, rankBasename +".ranks" );
		if( pr.numNodes < 100 ) {
			for( int i=0; i<pr.rank.length; i++ ) {
				System.err.println( "PageRank[" + i + "]=" + pr.rank[i] );
			}
		}
		Properties prop = pr.buildProperties( graphBasename, preferenceFilename, startFilename );
		prop.save( rankBasename + ".properties" );
		
		if ( order != null ) {
			System.err.print( "Saving derivatives..." );
			for( int i = 0; i < order.length; i++ ) BinIO.storeDoubles( pr.derivative[ i ], rankBasename + ".der-" + order[ i ] );
		}
		
		final double[] rank = pr.rank;
		pr = null;  // Let us free some memory...
		graph = null;
		
		if ( sorted ) {
			System.err.print( "Sorting ranks..." );
			Arrays.sort( rank );
			final int n = rank.length;
			int i = n / 2;
			double t;
			// Since we need the ranks from highest to lowest, we invert their order.
			while( i-- != 0 ) {
				t = rank[ i ];
				rank[ i ] = rank[ n - i - 1 ];
				rank[ n - i - 1 ] = t;
			}
			System.err.print( " saving sorted ranks..." );
			BinIO.storeDoubles( rank, rankBasename +"-sorted.ranks" );
			System.err.println( " done." );
		}
	}
}
