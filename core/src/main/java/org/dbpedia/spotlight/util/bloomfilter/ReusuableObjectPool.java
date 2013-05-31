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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Longer, faster Bloom filter
 *
 * From: http://code.google.com/p/java-longfastbloomfilter/
 * Licensed under Apache License 2.0
 */

public class ReusuableObjectPool<T extends Factory<T>> {

	private LinkedBlockingQueue<T> pool;
	private T factoryObject;
	private AtomicInteger numObjects = new AtomicInteger(1);
	private final int maxObjects;

	public ReusuableObjectPool(int maxObjects, T factoryObject) {
		pool = new LinkedBlockingQueue<T>(maxObjects);
		pool.add(factoryObject);
		this.factoryObject = factoryObject;
		this.maxObjects = maxObjects;
	}

	private T create() {
		numObjects.getAndIncrement();
		return factoryObject.create();
	}

	private void destroy(T o) {
		o.destroy();
		numObjects.getAndDecrement();
	}

	public T checkOut() {
		if (pool.isEmpty() && numObjects.get() < maxObjects) {
			return create();
		}

		try {
			return pool.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return create();
	}

	public void checkIn(T o) {
		if (!pool.offer(o)) {
			destroy(o);
		}
	}

	public void optimize(int predictedObjectsInUse) {
		int numToReduceBy = numObjects.get() - predictedObjectsInUse;
		while (numToReduceBy > 0) {
			T o = pool.poll();
			if (o == null) {
				break;
			} else {
				destroy(o);
				numToReduceBy--;
			}
		}
	}
}


