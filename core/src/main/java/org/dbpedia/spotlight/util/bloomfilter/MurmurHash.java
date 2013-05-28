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

public class MurmurHash {

    private static final long M = 0xc6a4a7935bd1e995L;
    private static final int R = 47;

    public long hash(final byte[] data, int length, long seed) {
        long h = (seed&0xffffffffL)^(length*M);

        int length8 = length/8;

        for (int i=0; i<length8; i++) {
            final int i8 = i*8;
            long k =  ((long)data[i8+0]&0xff)      +(((long)data[i8+1]&0xff)<<8)
                    +(((long)data[i8+2]&0xff)<<16) +(((long)data[i8+3]&0xff)<<24)
                    +(((long)data[i8+4]&0xff)<<32) +(((long)data[i8+5]&0xff)<<40)
                    +(((long)data[i8+6]&0xff)<<48) +(((long)data[i8+7]&0xff)<<56);

            k *= M;
            k ^= k >>> R;
            k *= M;

            h ^= k;
            h *= M;
        }

        switch (length%8) {
        case 7: h ^= (long)(data[(length&~7)+6]&0xff) << 48;
        case 6: h ^= (long)(data[(length&~7)+5]&0xff) << 40;
        case 5: h ^= (long)(data[(length&~7)+4]&0xff) << 32;
        case 4: h ^= (long)(data[(length&~7)+3]&0xff) << 24;
        case 3: h ^= (long)(data[(length&~7)+2]&0xff) << 16;
        case 2: h ^= (long)(data[(length&~7)+1]&0xff) << 8;
        case 1: h ^= (long)(data[length&~7]&0xff);
                h *= M;
        };

        h ^= h >>> R;
        h *= M;
        h ^= h >>> R;

        return h;
    }

}
