package org.dbpedia.spotlight.util.bloomfilter;

import java.io.Serializable;

/**
 * Longer, faster Bloom filter
 *
 * From: http://code.google.com/p/java-longfastbloomfilter/
 * Licensed under Apache License 2.0
 */

public class LongBitSet implements Cloneable, Serializable {

    private static final long serialVersionUID = 7997698588986878753L;
    /*
     * BitSets are packed into arrays of "words."  Currently a word is
     * a long, which consists of 64 bits, requiring 6 address bits.
     * The choice of word size is determined purely by performance concerns.
     */
    private final static int ADDRESS_BITS_PER_WORD = 6;

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private long[] words;

    /**
     * The number of bits in use
     */
    private long size = 0;

    /**
     * Given a bit index, return word index containing it.
     */
    private static int wordIndex(long bitIndex) {
        return (int) (bitIndex >> ADDRESS_BITS_PER_WORD);
    }

    /**
     * Creates a bit set whose initial size is large enough to explicitly
     * represent bits with indices in the range <code>0</code> through
     * <code>nbits-1</code>. All bits are initially <code>false</code>.
     *
     * @param     nbits   the initial size of the bit set.
     * @exception NegativeArraySizeException if the specified initial size
     *               is negative.
     */
    public LongBitSet(long nbits) {
        // nbits can't be negative; size 0 is OK
        if (nbits < 0)
            throw new NegativeArraySizeException("nbits < 0: " + nbits);

        initWords(nbits);

        this.size = nbits;
    }

    private void initWords(long nbits) {
        words = new long[wordIndex(nbits-1) + 1];
    }

    /**
     * Sets the bit specified by the index to <code>false</code>.
     *
     * @param     bitIndex   the index of the bit to be cleared.
     * @exception IndexOutOfBoundsException if the specified index is negative.
     */
    public void clear(long bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);

        words[wordIndex] &= ~(1L << bitIndex);
    }

    /**
     * Sets all of the bits in this BitSet to <code>false</code>.
     *
     */
    public void clear() {
        for (int word = 0; word < words.length; word++) {
            words[word] = 0;
        }
    }

    /**
     * Create a clone of this bit set, that is an instance of the same
     * class and contains the same elements.  But it doesn't change when
     * this bit set changes.
     *
     * @return the clone of this object.
     */
    public Object clone() {
        try {
            LongBitSet lbs = (LongBitSet) super.clone();
            lbs.words = (long[]) words.clone();
            return lbs;
        } catch (CloneNotSupportedException e) {
            // Impossible to get here.
            return null;
        }
    }
    /**
     * Returns the value of the bit with the specified index. The value
     * is <code>true</code> if the bit with the index <code>bitIndex</code>
     * is currently set in this <code>BitSet</code>; otherwise, the result
     * is <code>false</code>.
     *
     * @param     bitIndex   the bit index.
     * @return    the value of the bit with the specified index.
     * @exception IndexOutOfBoundsException if the specified index is negative.
     */
    public boolean get(long bitIndex) {
        int wordIndex = wordIndex(bitIndex);
        return ((words[wordIndex] & (1L << bitIndex)) != 0);
    }

    /**
     * Sets the bit at the specified index to <code>true</code>.
     *
     * @param     bitIndex   a bit index.
     * @exception IndexOutOfBoundsException if the specified index is negative.
     */
    public void set(long bitIndex) {
        int wordIndex = wordIndex(bitIndex);

        words[wordIndex] |= (1L << bitIndex);
    }

    /**
     * Sets the bits between from (inclusive) and to (exclusive) to true.
     *
     * @param from
     *            the start range (inclusive)
     * @param to
     *            the end range (exclusive)
     * @throws IndexOutOfBoundsException
     *             if from &lt; 0 || from &gt; to
     */
    public void set(int from, int to) {
        if (from < 0 || from > to)
            throw new IndexOutOfBoundsException();
        if (from == to)
            return;
        int lo_offset = from >>> ADDRESS_BITS_PER_WORD;
        int hi_offset = to >>> ADDRESS_BITS_PER_WORD;

        if (lo_offset == hi_offset) {
            words[hi_offset] |= (-1L << from) & ((1L << to) - 1);
            return;
        }

        words[lo_offset] |= -1L << from;
        words[hi_offset] |= (1L << to) - 1;
        for (int i = lo_offset + 1; i < hi_offset; i++)
            words[i] = -1;
    }

    /**
     * Returns the number of bits of space actually in use by this
     * <code>BitSet</code> to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return  the number of bits currently in this bit set.
     */
    public long size() {
        return size;
    }

}
