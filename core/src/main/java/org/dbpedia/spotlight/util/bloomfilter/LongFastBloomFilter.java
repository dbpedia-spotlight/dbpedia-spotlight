package org.dbpedia.spotlight.util.bloomfilter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Longer, faster Bloom filter
 *
 * From: http://code.google.com/p/java-longfastbloomfilter/
 * Licensed under Apache License 2.0
 */

public class LongFastBloomFilter {

    private LongBitSet longBitSet;
    private final int k;
    private long currentNumElements;

    private MurmurHash murmurHash;
    private long[] bitSetIndexes;
    private long hash1;
    private long hash2;

    static ICompactSerializer<LongFastBloomFilter> serializer = new LongFastBloomFilterSerializer();

    public static ICompactSerializer<LongFastBloomFilter> serializer() {
        return serializer;
    }

    public LongFastBloomFilter(int k, LongBitSet longBitSet) {
        this.k = k;
        this.longBitSet = longBitSet;
        currentNumElements = 0;
        bitSetIndexes = new long[k];
        murmurHash = new MurmurHash();
    }

    public static LongFastBloomFilter getFilter(long predictedNumElements, double falsePositiveProbability) {
        BloomFilterCalculations.BloomFilterSpecification bloomFilterSpec = BloomFilterCalculations.computeBloomFilterSpec(predictedNumElements, falsePositiveProbability);
        bloomFilterSpec = BloomFilterCalculations.optimizeBloomFilterForSpeed(bloomFilterSpec.K, bloomFilterSpec.bitSetSize, predictedNumElements, falsePositiveProbability);

        return new LongFastBloomFilter(bloomFilterSpec.K, new LongBitSet(bloomFilterSpec.bitSetSize));
    }

    private void setHashValues(byte[] element) {
        hash1 = murmurHash.hash(element, element.length, 0);
        hash2 = murmurHash.hash(element, element.length, hash1);
    }

    private void setBitSetIndexes(byte[] element) {
        for (int i = 0; i < k; i++) {
            bitSetIndexes[i] = (hash1 + i * hash2) % longBitSet.size();
        }
    }

    public void add(byte[] element) {
        setHashValues(element);
        setBitSetIndexes(element);
        for (long bitSetIndex : bitSetIndexes) {
            longBitSet.set((bitSetIndex < 0) ? bitSetIndex + longBitSet.size() : bitSetIndex);
        }
        currentNumElements++;
    }

    public boolean contains(byte[] element) {
        setHashValues(element);
        for (int i = 0; i < k; i++) {
            final long index = (hash1 + i * hash2) % longBitSet.size();
            if (!longBitSet.get((index < 0) ? index + longBitSet.size() : index)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the current false positive probability of the bloom filter based on how many
     * elements have been added to the filter.
     *
     * WARNING: this assumes that only unique values have been added to the bloom filter.
     * 'currentNumElements' is blindly incremented every time an element is added to the filter.
     * If duplicate elements are added this will report a higher false positive probability than
     * actually exists.
     */
    public double getCurrentFalsePositiveProbability() {
        // (1 - e^(-k * n / m)) ^ k
        return Math.pow((1 - Math.exp(-k * currentNumElements / (double)longBitSet.size())), k);
    }

    public long getCurrentNumberOfElements() {
        return currentNumElements;
    }

    public long getBitSetSize() {
        return longBitSet.size();
    }

    public int getNumHashFunctions() {
        return k;
    }

    public LongBitSet getLongBitSet() {
        return longBitSet;
    }

    public void clear() {
        currentNumElements = 0;
        longBitSet.clear();
    }
}

class LongFastBloomFilterSerializer implements ICompactSerializer<LongFastBloomFilter> {
    public void serialize(LongFastBloomFilter lbf, DataOutputStream dos) throws IOException {
        dos.writeInt(lbf.getNumHashFunctions());
        LongBitSetSerializer.serialize(lbf.getLongBitSet(), dos);
    }

    public LongFastBloomFilter deserialize(DataInputStream dis) throws IOException {
        int hashes = dis.readInt();
        LongBitSet lbf = LongBitSetSerializer.deserialize(dis);
        return new LongFastBloomFilter(hashes, lbf);
    }
}
