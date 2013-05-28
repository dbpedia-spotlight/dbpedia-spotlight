/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.util.bloomfilter;

/**
 * Longer, faster Bloom filter
 *
 * From: http://code.google.com/p/java-longfastbloomfilter/
 * Licensed under Apache License 2.0
 */

public class BloomFilterCalculations {

    public static final long MAX_BIT_ARRAY_SIZE = 137438953408L; //(long)Integer.MAX_VALUE*64;
    public static final double OPTIMAL_NUM_HASH_FUNCTIONS_MULTIPLIER = Math.log(2);
    public static final double ARRAY_SIZE_DENOMINATOR_CALC =  Math.pow(OPTIMAL_NUM_HASH_FUNCTIONS_MULTIPLIER,2);
    public static final double ELEMENT_SIZE_NUMERATOR_CALC = -MAX_BIT_ARRAY_SIZE * ARRAY_SIZE_DENOMINATOR_CALC;
    public static final double MAX_BITSET_SIZE_CHANGE = .2;

    /**
     * Returns the minimum bit array size (m) to satisfy the desired false
     * positive probability based on the number of elements expected for the
     * bloom filter.
     *
     * @param n - the number of expected elements for the bloom filter
     * @param falsePositiveProbability
     */
    public static long getMinBitArraySize(long n, double falsePositiveProbability) {
        // m = -(n * ln(p) / ln(2)^2)
        return (long) ((-n*Math.log(falsePositiveProbability))/ARRAY_SIZE_DENOMINATOR_CALC);
    }

    /**
     * Returns the maximum number of elements (n) the LongFastBloomFilter can hold
     * based on MAX_BIT_ARRAY_SIZE while still maintaining the desired false positive
     * probability.
     *
     * @param falsePositiveProbability
     */
    public static long maxNumberOfElementsForDesiredFalsePositiveProbability(double falsePositiveProbability) {
        // n = -(m * ln(2)^2) / ln(p), where m equals MAX_BIT_ARRAY_SIZE
        return (long) (ELEMENT_SIZE_NUMERATOR_CALC / Math.log(falsePositiveProbability));
    }

    public static int optimalNumberOfHashFunctions(long bitSetSize, long predictedNumElements) {
        // k = ln(2) * m/n
        return (int) (OPTIMAL_NUM_HASH_FUNCTIONS_MULTIPLIER * (bitSetSize / predictedNumElements));
    }

    /**
     * A wrapper class that holds two key parameters for a Bloom Filter: the
     * number of hash functions used, and the size of the bit set.
     */
    public static class BloomFilterSpecification {
        final int K; // number of hash functions.
        final long bitSetSize;

        public BloomFilterSpecification(int k, long bitSetSize) {
            K = k;
            this.bitSetSize = bitSetSize;
        }
    }

    public static BloomFilterSpecification computeBloomFilterSpec(long n, double p) {
        long m = 0;
        long maxNumElements = BloomFilterCalculations.maxNumberOfElementsForDesiredFalsePositiveProbability(p);
        if (n >= maxNumElements) {
            System.out.println("Max element size for a desired false positive probability of " + p + " is " + maxNumElements + ".");
            System.out.println("Setting predicted number of elements to " + maxNumElements);

            n = maxNumElements;
            m = MAX_BIT_ARRAY_SIZE;
        } else {
            m = BloomFilterCalculations.getMinBitArraySize(n, p);
        }

        int k = BloomFilterCalculations.optimalNumberOfHashFunctions(m, n);

        return new BloomFilterSpecification(k, m);
    }

    public static BloomFilterSpecification optimizeBloomFilterForSpeed(int k, long m, long n, double p) {
        // Sacrifice memory for speed:
        // By lowering the number of hash functions used, the .add() and .contains() methods
        // will be faster because there are less indexes to generate.  However, to maintain
        // the desired false positive probability we have to increase 'bitSetSize.'
        // The code below lowers k until either k is 0, the bitSetSize is larger than the
        // max allowed bit array size, or until the percent increase of the newly calculated
        // bit set size is larger than MAX_BITSET_SIZE_CHANGE of the previously
        // calculated optimal value of the bitSetSize.
        long tmpBitSetSize = m;
        long maxChange = (long) (MAX_BITSET_SIZE_CHANGE * m + m);
        int tmpK = k-1;
        while (tmpK > 0) {
        	// (-k * n) / ln(-p^(1/k) + 1)
            tmpBitSetSize = (long) ((-tmpK*n)/Math.log(-Math.pow(p,1/(double)tmpK)+1));

            if (tmpBitSetSize < MAX_BIT_ARRAY_SIZE && tmpBitSetSize < maxChange) {
                m = tmpBitSetSize;
                k = tmpK;
            } else {
                break;
            }
            tmpK--;
        }

        return new BloomFilterSpecification(k, m);
    }
}
