package es.yrbcn.graph.weighted;

/*
 * Copyright (C) 2004-2007 Paolo Boldi, Massimo Santini and Sebastiano Vigna
 * 
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

import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.law.rank.PageRank;
import it.unimi.dsi.util.Properties;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledNodeIterator;
import it.unimi.dsi.webgraph.labelling.Label;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

import org.apache.log4j.Logger;

// RELEASE-STATUS: DIST

/** A base abstract class definining methods and attributes supporting PageRank (or similar) computations.
 *  Includes features such as: settable preference vector, settable
 *  damping factor, programmable stopping criteria, step-by-step execution, reusability.
 *  <P>Users of this class should first create an instance specifying the graph over which PageRank should be computed.
 *  After doing this, the user may change the data used to compute PageRank (by manually setting the attributes
 *  {@link #alpha}, {@link #preference}, {@link #start}), and then (s)he may proceed in one of the following ways:
 *  <UL>
 *    <LI>the user can decide to call the {@link #init()} method, which initializes the state, and then (s)he may repeatedly call the
 *      {@link #step()} method; every call will compute the next approximation; the current approximation is contained
 *      in the {@link #rank} attribute;
 *    <LI>the user may directly call the {@link #stepUntil(PageRank.StoppingCriterion)} method, which iterates
 *      a call to {@link #step()} method until a certain stopping criterion is met; a {@link PageRank.StoppingCriterion}
 *      is a class that may decide if the iteration can be stopped (on the basis of a certain {@link PageRank} object).
 *		The {@link PageRank} class also provides two ready-to-use implementations of stopping criteria:
 * 		<UL>
 *          <LI>{@linkplain PageRank.NormDeltaStoppingCriterion one} that decides whether to stop depending on whether
 *      	the norm of the difference of two successive iterates is smaller than a certain threshold;
 *			The possible norms are given by <code>Norm?</code> type and the prefered norm can be choosen setting {@link #norm} before
 *			calling {@link #init()} method.
 *          <LI>{@linkplain PageRank.IterationNumberStoppingCriterion another} that decides whether to stop on the basis of the number of iterations.
 * 		</UL>
 *
 *   <P>Moreover, the {@link PageRank} class provides two static methods to compose two stopping criteria in a {@linkplain #and(PageRank.StoppingCriterion, PageRank.StoppingCriterion) conjunctive}
 *      or {@linkplain #or(PageRank.StoppingCriterion, PageRank.StoppingCriterion) disjunctive} way.
 *  </UL>
 *
 *  <P>At any time, the user may re-initialize the computation, by calling the {@link #init()} method, or (s)he may call the
 *  {@link #clear()} method that gets rid of the large arrays that the implementing classes usually manage. In the latter case, the arrays
 *  are rebuilt on the next call to {@link #init()}.
 *
 *  <h2>Formulae and preferences</h2>
 *
 *  <p>There are two main formulae for PageRank in the literature. The first one, which we shall
 *  call <em>weakly preferential</em>, patches all dangling nodes by adding a uniform transition towards
 *  all other nodes. The second one, which we shall call <em>strongly preferential</em>, patches all
 *  dangling nodes adding transitions weighted following the preference vector <var><b>v</b></var>.
 *  We can consider the two formulae together, letting <var><b>u</b></var> be a vector that is uniform
 *  in the weak case and coincides with <var><b>v</b></var> in the strong case.
 *
 *  <P>If we denote with <var>P</var> the normalised adjacency matrix of the graph, with <var><b>d</b></var>
 *  the characteristic vector of dangling nodes, and with &alpha; the damping factor, the weakly preferential
 *  equation is
 *  <div style="text-align: center">
 * <var><b>x</b></var>'= <var><b>x</b></var>' (&alpha; <var>P</var> + &alpha;<var><b>d</b></var><var><b>u</b></var>' +  (1-&alpha;)<b>1</b> <var><b>v</b></var>')
 * </div>
 *
 *  <p>By default, weakly preferential PageRank is computed; strongly preferential
 *  PageRank computation is enforced by setting {@link #stronglyPreferential} to true.
 *  In the {@link #init()} method the variable <code>preferentialAdjustment</code> is set to <code>null</code> iff
 *  weakly preferential PageRank should be computed, or to <code>preference</code>
 *  if strongly preferential PageRank should be computed.
 */

public abstract class WeightedPageRank {
	
	/** Default maximum number of iterations. */
	public final static int DEFAULT_MAX_ITER = Integer.MAX_VALUE;
	
	/** The default precision.
	 *
	 * We use double precision operands. Therefore the rounding error,
	 * always bounded by <em>machine epsilon</em>, can be lower to 1E-16 at most.
	 * The actual approximation error of rank values depends by the stop criterion adopted.
	 * For example, if the stop criterion is the L1 norm, under the approximation error uniform distribution assumption,
	 * a DEFAULT_THRESHOLD = 1E-6 permits to find rank values with 1E-6/{@link #numNodes} rounding error at most.
	 */
	public final static double DEFAULT_THRESHOLD = 1E-6;
	
	/**The admitted tolerance in the verification if a vector is a stochastic one.
	 *
	 * A stochastic vector has L1 norm equals to 1 &plusmn; STOCHASTIC_TOLERANCE.
	 */
	public final static double STOCHASTIC_TOLERANCE = 1E-5;
	
	/** The default damping factor. */
	public final static double DEFAULT_ALPHA = 0.85;
	
	/** Possible norms, with an {@linkplain Norm#compute(double[]) implementation}.  */
	public enum Norm {
		L1 {
			public double compute( double[] v ) {
				double s = 0;
				for( int i = v.length; i-- != 0; ) s += Math.abs( v[ i ] );
				return s;
			}
		},
		L2{
			public double compute( double[] v ) {
				double t, s = 0;
				for( int i = v.length; i-- != 0; ) {
					t = Math.abs( v[ i ] );
					s += t * t;
				}
				return Math.sqrt( s );
			}
		}, 
		INFTY {
			public double compute( double[] v ) {
				double t, s = 0;
				for( int i = v.length; i-- != 0; ) {
					t = Math.abs( v[ i ] );
					if ( t > s ) s = t;
				}
				return s;
			}
		};
		
		public abstract double compute( double[] v );
	}
	
	/** The alpha (damping) factor.
	 *
	 * In the random surfer interpretation, this is the probability that the
	 * surfer will follow a link in the current page. */
	public double alpha = DEFAULT_ALPHA;
	/** The preference vector to be used (or <code>null</code> if the uniform preference vector should be used). */
	public DoubleList preference;
	/** The starting vector to be used at the beginning of the PageRank algorithm (or <code>null</code> if the uniform starting vector should be used). */
	public DoubleList start;
	/** The vector used for preferential adjustment (<var>u</var> in the above general formula);
	 *  it coincides with the preference vector if strongly preferential PageRank is desired, or
	 *  to <code>null</code> otherwise.
	 */
	public DoubleList preferentialAdjustment;
	/** The underlying graph. */
	protected final ArcLabelledImmutableGraph g;
	/** The number of nodes of the underlying graph. */
	protected final int numNodes;
	/** If not <code>null</code>, the set of buckets of {@link #g}. */
	protected BitSet buckets;
	/** The current rank vector. */
	public double[] rank;
	/** The total out-weight of nodes. */
	public float[] sumoutweight;
	/** The current step number (0 after initialization). */
	public int iterationNumber;
	
	/** Decides whether we use the strongly or weakly preferential algorithm. */
	public boolean stronglyPreferential;
	
	/** Current norm
	 *
	 * {@link Norm#L1} is the default value.
	 */
	public Norm norm = Norm.L1;
	
	/** A logger defined by the concrete classes. */
	protected final Logger logger;
	
	/** Creates a new instance calculator with uniform start vector and uniform preference vector.
	 *
	 * @param g the graph.
	 * @param logger a logger.
	 */
	public WeightedPageRank( final ArcLabelledImmutableGraph g, final Logger logger ) {
		this.g = g;
		this.logger = logger;
		this.numNodes = g.numNodes();
		logger.info( "Graph dimension = " + numNodes );
	}
	
	
	/** A stopping criterion is a strategy that decides when a PageRank computation should be stopped. */
	public interface StoppingCriterion {
		/** Determines if the computation should be stopped.
		 *
		 * @param p the PageRank object.
		 * @return true iff the computation should be stopped.
		 */
		public boolean shouldStop( WeightedPageRank p );
	}
	
	/** A stopping criterion that stops whenever the number of iterations exceeds a given bound.
	 */
	public static class IterationNumberStoppingCriterion implements StoppingCriterion {
		private int maxIter;
		
		/** Creates an instance with a given number of iterations.
		 *
		 * @param maxIter the maximum number of iterations.
		 */
		public IterationNumberStoppingCriterion( final int maxIter ) {
			this.maxIter = maxIter;
		}
		
		public boolean shouldStop( final WeightedPageRank p ) {
			// If maxIter is infinity, we just return.
			if ( maxIter == Integer.MAX_VALUE ) return false;
			p.logger.info( "Iterations performed: " + p.iterationNumber + " (will stop after " + maxIter + ")" );
			return p.iterationNumber >= maxIter;
		}
	}
	
	/** A stopping criterion that evaluates the norm of the difference between the last two iterates, and stops
	 *  if this value is smaller than a given threshold.
	 */
	public static class NormDeltaStoppingCriterion implements StoppingCriterion {
		private double threshold;
		
		/** Creates an instance with given threshold.
		 *
		 * @param threshold the threshold.
		 */
		public NormDeltaStoppingCriterion( final double threshold ) {
			this.threshold = threshold;
		}
		
		public boolean shouldStop( final WeightedPageRank p ) {
			p.logger.info( "Current delta norm: " + p.normDelta() + " (will stop below " + threshold + ")" );
			return p.normDelta() < threshold;
		}
	}
	
	/** Composes two stopping criteria, producing a single stopping criterion (the computation stops iff both
	 *  conditions become true; lazy boolean evaluation is applied).
	 *
	 * 	@param stop1 a stopping criterion.
	 *  @param stop2 a stopping criterion.
	 *  @return a criterion that decides to stop as soon as both criteria are satisfied.
	 */
	public static StoppingCriterion and( final StoppingCriterion stop1, final StoppingCriterion stop2 ) {
		return new StoppingCriterion() {
			public boolean shouldStop( final WeightedPageRank p ) {
				return stop1.shouldStop( p ) && stop2.shouldStop( p );
			}
		};
	}
	
	/** Composes two stopping criteria, producing a single stopping criterion (the computation stops iff either
	 *  condition becomes true; lazy boolean evaluation is applied).
	 *
	 *  @param stop1 a stopping criterion.
	 *  @param stop2 a stopping criterion.
	 *  @return a criterion that decides to stop as soon as one of the two criteria is satisfied.
	 */
	public static StoppingCriterion or( final StoppingCriterion stop1, final StoppingCriterion stop2 ) {
		return new StoppingCriterion() {
			public boolean shouldStop( final WeightedPageRank p ) {
				return stop1.shouldStop( p ) || stop2.shouldStop( p );
			}
		};
	}
	
	/**
	 * Checks if the parameter is a stochastic vector: not negative value and 1.0 &plusmn; {@link #STOCHASTIC_TOLERANCE}
	 * L1-norm.
	 *
	 *@param v vector to check.
	 *@return true if the vector is a stochastic one.
	 */
	protected static boolean isStochastic( DoubleList v ) {
		double normL1 = 0.0, c = 0.0, t, y;
		int i;
		//Kahan method to minimize the round errors in doubles sum.
		for ( i = v.size(); i-- != 0 && v.getDouble( i ) >= 0; ) {
			y = v.getDouble( i ) - c;
			t = ( normL1 + y );
			c = ( t - normL1 ) - y;
			normL1 = t;
		}
		System.err.println( "NORM is " + normL1 );
		return ( i == -1 && Math.abs( normL1 - 1.0 ) <= STOCHASTIC_TOLERANCE );
	}
	
	/**
	 * Returns a Properties object that contains all the parameters used by the computation.
	 *
	 * @param graphBasename file name of the graph
	 * @param preferenceFilename file name of preference vector. It can be null.
	 * @param startFilename file name of the eventually start vector.
	 * @return a properties object that represent all the parameters used to calculate the rank.
	 */
	public Properties buildProperties( String graphBasename, String preferenceFilename, String startFilename ) {
		
		Properties prop = new Properties();
		prop.addProperty( "rank.alpha", Double.toString( alpha ) );
		prop.addProperty( "rank.stronglyPreferential", stronglyPreferential );
		prop.addProperty( "method.numberOfIterations", iterationNumber );
		prop.addProperty( "method.norm.type", norm );
		prop.addProperty( "method.norm.value", Double.toString( normDelta() ) );
		prop.addProperty( "graph.nodes", numNodes );
		prop.addProperty( "graph.fileName", graphBasename );
		prop.addProperty( "preference.fileName", preferenceFilename );
		prop.addProperty( "start.fileName", startFilename );
		return prop;
	}
	
	/** Initializes the variables for PageRank computation.
	 *
	 * <p>This method initialises the starting vector
	 */
	@SuppressWarnings("unused")
	public void init() throws IOException {
		logger.info( "Initialising..." );
		logger.info( "alpha = " + alpha );
		logger.info( "norm type = " + norm );

		iterationNumber = 0;
		
		// Creates the array, if necessary
		if ( rank == null ) rank = new double[ numNodes ];
		
		// Initializes the rank array
		if ( start != null ) {
			if ( ! isStochastic( start ) ) throw new IllegalArgumentException( "The start vector is not a stochastic vector." );
			start.toDoubleArray( rank );
		} else {
			if ( preference != null ) preference.toDoubleArray( rank );
			else DoubleArrays.fill( rank, 1.0 / numNodes );
		}
		logger.info( "There is a start vector = " + ( start != null ) );
		
		// Check the preference vector
		if ( preference != null ) {
			if ( preference.size() != numNodes ) throw new IllegalArgumentException( "The preference vector size (" + preference.size() + ") is different from graph dimension (" + numNodes + ")." );
			if ( !isStochastic( preference) ) throw new IllegalArgumentException( "The preference vector is not a stochastic vector. " );
		}
		logger.info( "There is a preference vector = " + ( preference != null ) );
		
		// Initializes the preferentialAdjustment vector
		if ( stronglyPreferential ) {
			if ( preference == null ) throw new IllegalArgumentException( "The strongly flag is true but the preference vector is null." );
			preferentialAdjustment = preference;
		} else preferentialAdjustment = null;
		logger.info( "Strongly preferential = " + stronglyPreferential );
		
//		 Reads sum of out-weights in main memory 
		if ( sumoutweight == null ) {
			logger.info( "Reading graph once to get sum of outweights" );
			sumoutweight = new float[ numNodes ];
			Arrays.fill( sumoutweight, (float)0 );
			ArcLabelledNodeIterator nodeIterator = g.nodeIterator();

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
					sumoutweight[curr] += lab[j].getFloat();
				}
			}
			
		}

	}
	
	
	/** Performs one computation step.
	 */
	public abstract void step() throws IOException;
	
	/** Returns a norm of the difference with the previous step rank vector. The
	 * kind of norm is usually established in the constructor.
	 *
	 * @return a norm of the difference with the previous step rank vector.
	 * @throws IllegalStateException if called before the first iteration.
	 * @throws UnsupportedOperationException if it is not possible to compute a norm.
	 */
	public double normDelta() {
		throw new UnsupportedOperationException();
	}
	
	/** Calls {@link #init()} and steps until a given stopping criterion is met.
	 * The criterion is checked <i>a posteriori</i> (i.e., after each step); this means that
	 * at least one step is performed.
	 *
	 * @param stoppingCriterion the stopping criterion to be used.
	 * @throws IOException if an exception occurs during computation.
	 */
	public void stepUntil( final StoppingCriterion stoppingCriterion ) throws IOException {
		init();
		do step(); while ( !stoppingCriterion.shouldStop( this ) );
		logger.info( "Computation completed." );
	}
	
	/** Clears all data. After calling this method, data about the last PageRank computations are cleared, and you should
	 *  call again {@link #init()} before computing PageRank again.
	 */
	public void clear() {
		rank = null;
	}
	
}