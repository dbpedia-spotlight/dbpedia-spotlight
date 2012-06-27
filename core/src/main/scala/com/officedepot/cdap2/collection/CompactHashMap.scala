package com.officedepot.cdap2.collection

import FixedHashSet._

/** <p>
 *  This class implements mutable maps using a hashtable.
 *  </p>
 *  <p>
 *  Implementation is very memory-compact, especially on primitive types.
 *  CompactHashMap[Int,Int] with 1 million elements consumes ~16Mb of memory.
 *  Standard java/scala mutable HashMaps consumes ~46-60Mb on 32-bit platform
 *  and ~80-100Mb on 64-bit platform.
 *  </p>
 *  <p>
 *  <b>Not</b> thread-safe!
 *  </p>
 *  <p>
 *  Preserves iteration order until no elements are deleted.
 *  </p>
 *  <p>
 *  <code>null</code> is valid for both key and value.
 *  </p>
 *
 *  @author  Alex Yakovlev
 */
object CompactHashMap {

  /** Construct an empty CompactHashMap.*/
  def apply[K: ClassManifest, V: ClassManifest] = new CompactHashMap[K,V]

  /** Construct an empty map with given key and value classes and initial capacity. */
  def apply[K: ClassManifest ,V: ClassManifest] (capacity: Int) =
    new CompactHashMap[K,V] (capacity)

  /** Construct an empty map with given key and value classes, initial capacity, and load factor. */
  def apply[K: ClassManifest ,V: ClassManifest] (capacity: Int, loadFactor: Float) =
    new CompactHashMap[K,V] (capacity, loadFactor)

  /** Construct an empty map with given elements. */
  def apply[K: ClassManifest ,V: ClassManifest] (elems: (K,V)*) =
    (new CompactHashMap[K,V] /: elems) {
      (m,p) => m update (p._1, p._2); m }
}

/** Mutable CompactHashMap */
@cloneable
class CompactHashMap[K: ClassManifest, V: ClassManifest] () extends scala.collection.mutable.Map[K,V] with Serializable {

  private def this (keys: FixedHashSet[K], values: Array[V]) = {
    this ()
    myKeys = keys
    myValues = values
  }

  def this (capacity: Int) = {
    this ()
    var bits = initialBits
    while ((1 << bits) < capacity) bits += 1
    myKeys = FixedHashSet (bits)
    myValues = newArray (myKeys.capacity)
  }

  def this (capacity: Int, loadFactor: Float) = {
    this ()
    var bits  = initialBits
    while ((1 << bits) < capacity) bits += 1
    myKeys = FixedHashSet (bits, loadFactor)
    myValues = newArray (myKeys.capacity)
  }

  /** FixedHashSet with this map's keys.
   */
  private[this] var myKeys = EMPTY_HASH_SET.asInstanceOf[FixedHashSet[K]]

  /** Array with this map's values.
   */
  private[this] var myValues: Array[V] = null

  /** Is the given key mapped to a value by this map?
   *
   *  @param   key  the key
   *  @return  <code>true</code> if there is a mapping for key in this map
   */
  override def contains (key: K) = myKeys.positionOf(key) >= 0

  /** Is the given integer key mapped to a value by this map?
   *
   *  @param   key  the key
   *  @return  <code>true</code> if there is a mapping for key in this map
   */
  def containsInt (key: Int) = myKeys.positionOfInt(key) >= 0

  /** Check if this map maps <code>key</code> to a value and return the
   *  value if it exists.
   *
   *  @param   key  the key of the mapping of interest
   *  @return  the value of the mapping, if it exists
   */
  def get (key: K): Option[V] = {
    val i = myKeys.positionOf(key)
    if (i >= 0) Some(myValues(i)) else None
  }

  /** Retrieve the value which is associated with the given key.
   *  If there is no mapping from the given key to a value,
   *  default(key) is returned (currenly throws an exception).
   *
   *  @param   key  the key
   *  @return  the value associated with the given key.
   */
  override def apply (key: K): V = {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) else default(key)
  }

  /** Retrieve the value which is associated with the given integer key.
   *  If there is no mapping from the given key to a value,
   *  default(key) is returned (currenly throws an exception).
   *
   *  @param   key  the key
   *  @return  the value associated with the given key.
   */
  def applyInt (key: Int): V = {
    val i = myKeys.positionOfInt(key)
    if (i >= 0) myValues(i) else default(key.asInstanceOf[K])
  }

  /** Check if this map maps <code>key</code> to a value.
   *  Return that value if it exists, otherwise return <code>default</code>.
   */
  override def getOrElse[V2 >: V] (key: K, default: => V2): V2 = {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) else default
  }

  /** Check if this map maps <code>key</code> to a value.
   *  Return that value if it exists, otherwise return <code>default</code>.
   */
  def getOrElseF[V2 >: V] (key: K, default: () => V2): V2 = {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) else default()
  }

  /** Check if this map maps <code>key</code> to a value.
   *  Return that value if it exists, otherwise return <code>default</code>.
   */
  def getOrElseV[V2 >: V] (key: K, default: V2): V2 = {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) else default
  }

  /** Returns the size of this hash map.
   */
  override def size = myKeys.size

  /** Removes all elements from the map.
   *  After this operation is completed, the map will be empty.
   */
  override def clear {
    myKeys.clear
    if (myValues ne null) {
      val len = myValues.length
      var i = 0
      while (i < len) {
        myValues(i) = null.asInstanceOf[V]
        i += 1
      }
    }
  }

  /** Resize map. */
  private[this] def resize (key: K, value: V, bits: Int) {
    if (myValues ne null) {
      myKeys   = FixedHashSet (bits, myKeys)
      myValues = resizeArray (myValues, myKeys.capacity)
    } else {
      myKeys   = FixedHashSet (bits, myKeys.loadFactor)
      myValues = newArray (myKeys.capacity)
    }
  }

  /** This method allows one to add a new mapping from <code>key</code>
   *  to <code>value</code> to the map. If the map already contains a
   *  mapping for <code>key</code>, it will be overridden by this
   *  function.
   *
   * @param  key    The key to update
   * @param  value  The new value
   */
  override def update (key: K, value: V) =
    try {
      val i = myKeys.add (key)
      myValues(i) = value
    } catch {
      case ResizeNeeded =>
        resize (key, value, myKeys.bits + 1)
        val i2 = myKeys.addNew (key)
        myValues(i2) = value
    }

  // RPR
  def += (kv: (K, V)) = {
    val (key, value) = kv
    update (key, value)
    this
  }


  /** This method allows one to add a new mapping from integer <code>key</code>
   *  to <code>value</code> to the map. If the map already contains a
   *  mapping for <code>key</code>, it will be overridden by this
   *  function.
   *
   * @param  key    The key to update
   * @param  value  The new value
   */
  def updateInt (key: Int, value: V) =
    try {
      val i = myKeys.addInt (key)
      myValues(i) = value
    } catch {
      case ResizeNeeded =>
        val boxedKey = key.asInstanceOf[K]
        resize (boxedKey, value, myKeys.bits + 1)
        val i2 = myKeys.addNew (boxedKey)
        myValues(i2) = value
    }

  /** Insert new key-value mapping or update existing with given function.
   *
   * @param  key  The key to update
   * @param  newValue  The new value
   * @param  updateFunction  Function to apply to existing value
   */
  def insertOrUpdate (key: K, newValue: => V, updateFunction: V => V) {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) = updateFunction (myValues(i))
    else {
      val newV = newValue
      try {
        val j = myKeys.addNew (key)
        myValues(j) = newV
      } catch {
        case ResizeNeeded =>
          resize (key, newV, myKeys.bits + 1)
          val j = myKeys.addNew (key)
          myValues(j) = newV
      }
    }
  }

  /** Insert new key-value mapping or update existing with given function.
   *
   * @param  key  The key to update
   * @param  newValue  Function to get new value
   * @param  updateFunction  Function to apply to existing value
   */
  def insertOrUpdateF (key: K, newValue: () => V, updateFunction: V => V) {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) = updateFunction (myValues(i))
    else {
      val newV = newValue ()
      try {
        val j = myKeys.addNew (key)
        myValues(j) = newV
      } catch {
        case ResizeNeeded =>
          resize (key, newV, myKeys.bits + 1)
          val j = myKeys.addNew (key)
          myValues(j) = newV
      }
    }
  }

  /** Insert new key-value mapping or update existing with given function.
   *
   * @param  key  The key to update
   * @param  newValue  Function to get new value
   * @param  updateFunction  Function to apply to existing value
   */
  def insertOrUpdateV (key: K, newValue: V, updateFunction: V => V) {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) = updateFunction (myValues(i))
    else try {
      val j = myKeys.addNew (key)
      myValues(j) = newValue
    } catch {
      case ResizeNeeded =>
        resize (key, newValue, myKeys.bits + 1)
        val j = myKeys.addNew (key)
        myValues(j) = newValue
    }
  }

  /** Remove a key from this map, noop if key is not present.
   *
   *  @param  key  the key to be removed
   */
  override def -= (key: K): this.type = {
    val i = myKeys.delete (key)
    if (myValues.isInstanceOf[Array[AnyRef]] && i >= 0)
      myValues (i) = null.asInstanceOf[V]
    this
  }

  /**
   * Creates an iterator for all key-value pairs.
   *
   *  @return  an iterator over all key-value pairs.
   */
  def iterator: Iterator[(K,V)] = myKeys.elementsMap { (k,i) => (k -> myValues(i)) }

  // RPR
  //override def elements = iterator

  /**
   * Creates an iterator for a contained values.
   *
   *  @return  an iterator over all values.
   */
  override def values: Iterable[V] = myValues

  /**
   * Map keys
   *
   * @return  an iterator over all keys.
   */
  override def keys: Iterable[K] = myKeys

  /** Set of this map keys.
   *
   * @return the keys of this map as a set.
   */
  override def keySet: scala.collection.Set[K] = myKeys

  /** Return a clone of this map.
   *
   *  @return a map with the same elements.
   */
  override def clone = {
    // RPR val c = super.clone.asInstanceOf[CompactHashMap[K,V]]
    // c.cloneData
    // c
    new CompactHashMap[K,V] (myKeys.clone, if (myValues ne null) resizeArray (myValues, myValues.length) else myValues)
  }

  /** Clone internal data declared as private[this]
   */
  private def cloneData {
    myKeys = myKeys.clone
    if (myValues ne null) myValues = resizeArray (myValues, myValues.length)
  }

  /** Returns a new map containing all elements of this map that
   *  satisfy the predicate <code>p</code>.
   *
   *  @param   p  the predicate used to filter the map.
   *  @return  the elements of this map satisfying <code>p</code>.
   */
  override def filter (p: ((K,V)) => Boolean) = {
    var newValues: Array[V] = null
    val newKeys = myKeys.filter (
      new Filter[K] {
        def check (key: K, i: Int) = p (key, myValues(i))
        def create (size: Int) { if (size > 0) newValues = newArray (size) }
        def copy (i: Int, j: Int) { newValues(i) = myValues(j) }
      })
    new CompactHashMap (newKeys, newValues)
  }

  /** Returns a new map containing all elements of this map that
   *  satisfy the predicate <code>p</code> (without Tuple2).
   *
   *  @param   p  the predicate used to filter the map.
   *  @return  the elements of this map satisfying <code>p</code>.
   */
  def filter (p: (K,V) => Boolean) = {
    var newValues: Array[V] = null
    val newKeys = myKeys.filter (
      new Filter[K] {
        def check (key: K, i: Int) = p (key, myValues(i))
        def create (size: Int) { if (size > 0) newValues = newArray (size) }
        def copy (i: Int, j: Int) { newValues(i) = myValues(j) }
      })
    new CompactHashMap (newKeys, newValues)
  }

  /** Converts this map to a fresh Array with elements.
   */
  def toArray = {
    val a = new Array[(K,V)] (myKeys.size)
    var i = 0
    iterator foreach { x => a{i} = x; i += 1 }
    a
  }

  /** Converts this map to a fresh List with elements.
   */
  override def toList = myKeys.toListMap { (k,i) => (k,myValues(i)) }
}