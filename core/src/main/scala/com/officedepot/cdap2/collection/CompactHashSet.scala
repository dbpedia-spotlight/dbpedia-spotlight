package com.officedepot.cdap2.collection

import FixedHashSet._

/** <p>
 *  This class implements mutable sets using a hashtable.
 *  </p>
 *  <p>
 *  Implementation is very memory-compact, especially on primitive types.
 *  </p>
 *  <p>
 *  <b>Not</b> thread-safe!
 *  </p>
 *  <p>
 *  Preserves iteration order until no elements are deleted.
 *  </p>
 *  <p>
 *  <code>null</code> is a valid element value.
 *  </p>
 *
 *  @author  Alex Yakovlev
 */
object CompactHashSet {

  /** Construct an empty CompactHashSet.
   */
  def apply[T: ClassManifest] () = new CompactHashSet[T]

  /** Construct an empty set with given elements class
   *  and initial capacity.
   */
  def apply[T: ClassManifest] (capacity: Int) =
    new CompactHashSet (capacity)

  /** Construct an empty set with given elements class,
   *  initial capacity, and load factor.
   */
  def apply[T: ClassManifest] (capacity: Int, loadFactor: Float) =
    new CompactHashSet (capacity, loadFactor)

  /** Construct an empty set with given elements.
   */
  def apply[T: ClassManifest] (elems: T*) =
    (new CompactHashSet[T] /: elems) { (s,p) =>
      s += p
      s
    }
}

/**
 */
@cloneable
class CompactHashSet[T: ClassManifest] () extends scala.collection.mutable.Set[T]  with Serializable {

  def this (set: FixedHashSet[T]) = {
    this ()
    fixedSet = set
  }

  def this (capacity: Int) = {
    this ()
    var bits = initialBits
    while ((1 << bits) < capacity) bits += 1
    fixedSet = FixedHashSet (bits)
  }

  def this (capacity: Int, loadFactor: Float) = {
    this ()
    var bits = initialBits
    while ((1 << bits) < capacity) bits += 1
    fixedSet = FixedHashSet (bits, loadFactor)
  }

  /** Array to hold this set elements.
   */
  private[this] var fixedSet = EMPTY_HASH_SET.asInstanceOf[FixedHashSet[T]]

  /**
   * Check if this set contains element <code>elem</code>.
   *  @param  elem  the element to check for membership.
   *  @return  <code>true</code> if <code>elem</code> is contained in this set.
   */
  def contains (elem: T) = fixedSet.positionOf (elem) >= 0

  /**
   * Add a new element to the set.
   *  @param  elem  the element to be added
   */
  def += (elem: T): this.type = try {
    fixedSet.add (elem)
    this
  } catch {
    case ResizeNeeded => {
      fixedSet = FixedHashSet (fixedSet.bits + 1, fixedSet)
      fixedSet.add (elem)
      this
    }
  }

  /** Returns the size of this hash set.*/
  override def size = fixedSet.size

  /**
   * Creates an iterator for all set elements.
   * @return  an iterator over all set elements.
   */
  //override def elements = fixedSet.elements

  // RRR verify

  def iterator: Iterator[T] = fixedSet.iterator

  /**
   * Removes a single element from a set.
   *  @param  elem  The element to be removed.
   */
  def -= (elem: T): this.type = {
    fixedSet.delete (elem)
    this
  }

  override def - (elem: T): this.type  = {
    fixedSet.delete (elem)
    this
  }

  /** Return a clone of this set.
   *
   *  @return  a set with the same elements.
   */
  override def clone (): CompactHashSet[T] = {
    // RPR
    // val c: CompactHashSet[T] = super.clone.asInstanceOf[CompactHashSet[T]]
    // c.cloneData
    // c
    new CompactHashSet[T] (fixedSet.clone)
  }

  /** Clone internal data declared as private[this] */
  private def cloneData {
    fixedSet = fixedSet.clone
  }

  /**
   * Returns a new set containing all elements of this set that
   *  satisfy the predicate <code>p</code>.
   *
   *  @param   p  the predicate used to filter the set.
   *  @return  the elements of this set satisfying <code>p</code>.
   */
  override def filter (p: T => Boolean) =
    new CompactHashSet (fixedSet.filter (
      new Filter[T] {
        def check (e:T, i:Int) = p(e)
        def create (size: Int) { }
        def copy (i:Int, j:Int) { }
      }))

  /**
   * Removes all elements from the set.
   *  After this operation is completed, the set will be empty.
   */
  override def clear { fixedSet.clear }

  /** New List with this set elements. */
  override def toList = fixedSet.toList
}

/** Hash set backed by fixed size array. */
@cloneable
private abstract class FixedHashSet[T: ClassManifest] (final val bits: Int,
                                                       private[this] var array: Array[T],
                                                       final val loadFactor: Float) extends scala.collection.Set[T] with Serializable {

  /** Index of the first element in array with given hash. */
  protected def firstIndex (i: Int): Int

  /**
   * Method to access a linked list of elements
   *  in array with the same hash code.
   *
   *  Non-negative values are indices of the next element,
   *  -1 (default) is empty spot (or end of deleted list),
   *  -2 is 'end of list',
   *  other negative values are indices of next deleted element.
   */
  protected def nextIndex (i: Int): Int

  /** Set index of the first element in array with given hash. */
  protected def setFirstIndex (i: Int, v: Int)

  /** Update linked list of elements with equal hash. */
  protected def setNextIndex (i: Int, v: Int)

  /** Mask for hashcode to store in empty index bits. */
  def hcBitmask: Int

  /** End of list bit or 0 if it's not supported. */
  def eolBitmask = 0

  /** Index value to mark the end of deleted list. */
  def deletedEOL = -1

  /** Return index of elem in array or -1 if it does not exists. */
  def positionOf[B >: T] (elem: B): Int

  /** Return index of integer elem in array or -1 if it does not exists. */
  def positionOfInt (elem: Int) = positionOf (elem)

  /** */
  final def arrayLength = if (array eq null) 0 else array.length

  /** */
  final def hashLength = 1 << bits

  /** */
  final def indexBitmask = (1 << bits) -1

  /** Number of elements in this set */
  private[this] var counter = 0

  /** Starting index of empty elements in array */
  private[this] var firstEmptyIndex = 0

  /** Index of first deleted elements list in array */
  private[this] var firstDeletedIndex = -1

  /** Number of elements in this set. */
  final override def size = counter

  /** Maximum number of elements in this set's array. */
  final def capacity = arrayLength

  /** Set size (for internal use only). */
  final protected def setSize (newSize: Int) {
    counter = newSize
    firstEmptyIndex = newSize
    firstDeletedIndex = -1
  }

  /** Array with this set elements.*/
  final def getArray = array

  /** Array with this linked list indices. */
  protected def getIndexArray: AnyRef

  /** Return true if given array index is empty (or deleted). */
  def isUnoccupied (i: Int): Boolean

  /** Return index in array to insert new element. */
  protected final def findEmptySpot = {
    if (counter >= arrayLength) throw ResizeNeeded
    counter += 1
    if (firstDeletedIndex >= 0) {
      val i = firstDeletedIndex
      firstDeletedIndex = nextIndex (i)
      if (firstDeletedIndex < -2 && firstDeletedIndex != deletedEOL)
        firstDeletedIndex = -3-firstDeletedIndex
      setNextIndex (i, -1)
      i
    } else {
      val i = firstEmptyIndex
      firstEmptyIndex += 1
      i
    }
  }

  //RPR FIX ME Should be new Set with element.
  def + (elem: T): this.type = {
    add (elem)
    this
  }

  /**
   * Adds element to set.
   * Throws ResizeNeeded if set is full.
   * @return  index of inserted element in array
   */
  def add (elem: T): Int

  /**
   * Adds integer element to set.
   * Throws ResizeNeeded if set is full.
   * @return  index of inserted element in array
   */
  def addInt (elem: Int): Int = add (elem.asInstanceOf[T])

  /**
   * Adds element to set that does not already exist in this set.
   * Throws ResizeNeeded if set is full.
   * @return  index of inserted element in array
   */
  final def addNew (elem: T) = {
    val newIndex = findEmptySpot
    val hc = hash (elem)
    val i = hc & indexBitmask
    // Sorry, it was a test if inheritance instead of association will work...
    // getIndexArray match {
    //   case ia: Array[Int] =>
    //     val next = ia (i)
    //     ia (i) = ~(newIndex | (hc & hcBitmask) | (if (next < 0) 0 else INT_END_OF_LIST))
    //     if (next < 0) ia (hashLength + newIndex) = next
    //   case ia: Array[Short] =>
    //     val next = ia (i)
    //     ia (i) = (~newIndex ^ (hc & hcBitmask)).asInstanceOf[Short]
    //     ia (hashLength + newIndex) = if (next < 0) next else 1
    //   case ia: Array[Byte] =>
    //     val next = ia (i)
    //     ia (i) = (~newIndex ^ (hc & hcBitmask)).asInstanceOf[Byte]
    //     ia (hashLength + newIndex) = if (next < 0) next else 1
    // }

    val ia = getIndexArray
    if (ia.isInstanceOf[Array[Int]]) {
      val iai: Array[Int] = ia.asInstanceOf[Array[Int]]
      val next = iai (i)
      iai (i) = ~(newIndex | (hc & hcBitmask) | (if (next < 0) 0 else INT_END_OF_LIST))
      if (next < 0) iai (hashLength + newIndex) = next
    } else if (ia.isInstanceOf[Array[Short]]) {
      val ias: Array[Short] = ia.asInstanceOf[Array[Short]]
      val next = ias (i)
      ias (i) = (~newIndex ^ (hc & hcBitmask)).asInstanceOf[Short]
      ias (hashLength + newIndex) = if (next < 0) next else 1
    } else if (ia.isInstanceOf[Array[Byte]]) {
      val iab: Array[Byte] = ia.asInstanceOf[Array[Byte]]
      val next = iab (i)
      iab (i) = (~newIndex ^ (hc & hcBitmask)).asInstanceOf[Byte]
      iab (hashLength + newIndex) = if (next < 0) next else 1
    }
    array (newIndex) = elem
    newIndex
  }

  /**
   * Copy all elements from another FixedHashSet.
   *
   * @param  that  another set to copy elements from.
   * @param  callback  function to call for each copied element
   *                   with its new and old indices in set's arrays.
   */
  def rehash (that: FixedHashSet[T])

  /**
   * Make copy of this set filtered by predicate.
   *
   * @param  p  Predicate to test elements.
   * @param  newCallback  function to call when new set is created
   *                      with number of bits in new set's array.
   * @param  copyCallback  function to call for each copied element
   *                    with its new and old indices in set's arrays.
   */
  final def filter (f: Filter[T]): FixedHashSet[T] = {
    // First, test all element with predicate,
    // count, and store test results in a bit set.
    val bitSet = new Array[Long] (1 max (hashLength >>> 6))
    var count = 0
    var newBits = initialBits
    var newLen = ((1 << newBits) * loadFactor).asInstanceOf[Int]
    var i = 0
    while (i < firstEmptyIndex) {
      if ((firstDeletedIndex < 0 || !isUnoccupied (i)) && f.check (array(i),i)) {
        bitSet(i >>> 6) |= 1L << (i & 63)
        count += 1
        if (count > newLen) {
          newBits += 1
          newLen = ((1 << newBits) * loadFactor).asInstanceOf[Int]
        }
      }
      i += 1
    }

    // Now we can allocate set with exact size.
    if (count == 0) {
      f.create (-1)
      EMPTY_HASH_SET.asInstanceOf[FixedHashSet[T]]
    } else {
      val c: FixedHashSet[T] = FixedHashSet (newBits, loadFactor)
      f.create (c.capacity)
      i = 0
      while (i < firstEmptyIndex) {
        if ((bitSet(i >>> 6) >>> (i & 63) & 1) != 0) {
          val i2 = c.addNew (array(i))
          f.copy (i2, i)
        }
        i += 1
      }
      c
    }
  }

  /** Make complete copy of this set.
   */
  override def clone = {
    val c = super.clone.asInstanceOf[FixedHashSet[T]]
    c.cloneData
    c
  }

  /** Clone internal data declared as private[this] */
  protected def cloneData {
    if (array ne null) array = resizeArray (array, array.length)
  }

  /** Removes all elements from the set.
   */
  def clear {
    var i = 0
    if (array.isInstanceOf[Array[AnyRef]])
      while (i < firstEmptyIndex) {
        array (i) = null.asInstanceOf[T]
        i += 1
      }
    counter = 0
    firstEmptyIndex = 0
    firstDeletedIndex = -1
  }

  //RPR - Should copy and return a new Set with deleted element.
  final def - (elem: T)  = {
    delete (elem)
    this
  }

  /**
   * Delete element from set.
   * @return  index of deleted element in array
   *          or negative values if it was not present in set.
   */
  final def delete (elem: T): Int = {
    val hc = hash(elem)

    val mask = hcBitmask
    val hcBits = hc & mask
    val eol = eolBitmask

    var prev = -1
    var curr = hc & indexBitmask
    while (true) {
      val i = firstIndex (curr)
      if (i < 0) return -1
      val j = i & indexBitmask
      val k = hashLength + j

      if (hcBits == (i & mask)) {
        val o = array(j)
        if ((elem.asInstanceOf[Object] eq o.asInstanceOf[Object]) || elem == o) {
          counter -= 1
          if (array.isInstanceOf[Array[AnyRef]])
            array (j) = null.asInstanceOf[T]
          //
          if ((i & eol) == 0)
            setFirstIndex (curr, firstIndex (k))
          else if (prev >= 0)
            setFirstIndex (prev, firstIndex(prev) ^ eol)
          else
            setFirstIndex (curr, -1)
          /* We are limited in index domain, e.g. for Byte:
           0-127 = next element indices
           -1 = empty (array default)
           -2 = end of list
           -128 to -3 = next empty (deleted) index
           So we handle 2 last positions specially */
          if (j == firstEmptyIndex-1) {
            firstEmptyIndex = j
            setNextIndex (j, deletedEOL)
          } else if (firstDeletedIndex == hashLength-2) {
            // arrayLength-2 is out of NextIndex range
            // and can only be pointed to with firstDeletedIndex,
            // so we need to update next deleted list element
            setNextIndex (j, nextIndex(firstDeletedIndex))
            setNextIndex (firstDeletedIndex, -3-j)
          } else {
            setNextIndex (j, if (firstDeletedIndex < 0) deletedEOL else -3-firstDeletedIndex)
            firstDeletedIndex = j
          }
          return j
        }
      }
      //
      if ((i & eol) != 0) return -1
      prev = curr
      curr = k
    }
    -1
  }


  /**
   * Iterator for this set elements.
   */
  final override def iterator = new Iterator[T] {
    private[this] var i = 0
    def hasNext = {
      while (i < firstEmptyIndex && firstDeletedIndex >= 0 && isUnoccupied (i)) i += 1
      i < firstEmptyIndex
    }
    def next = {
      while (i < firstEmptyIndex && firstDeletedIndex >= 0 && isUnoccupied (i)) i += 1
      if (i < firstEmptyIndex) { i += 1; array(i-1) }
      else Iterator.empty.next
    }
  }

  /**
   * Iterate through this set elements with function.
   */
  final def elementsMap[F] (f: (T,Int) => F) = new Iterator[F] {
    private[this] var i = 0
    def hasNext = {
      while (i < firstEmptyIndex && firstDeletedIndex >= 0 && isUnoccupied (i)) i += 1
      i < firstEmptyIndex
    }
    def next = {
      while (i < firstEmptyIndex && firstDeletedIndex >= 0 && isUnoccupied (i)) i += 1
      if (i < firstEmptyIndex) { i += 1; f (array(i-1), i-1) }
      else Iterator.empty.next
    }
  }

  /** Return <code>true</code> if set contains element <code>elem</code>. */
  final def contains (elem: T) = positionOf (elem) >= 0

  /** List of this set elements. */
  final override def toList = {
    var list = List[T] ()
    var i = firstEmptyIndex
    while (i > 0) {
      i -= 1
      if (firstDeletedIndex < 0 || !isUnoccupied (i))
        list = array(i) :: list
    }
    list
  }

  /** List of this set elements. */
  final def toListMap[F] (f: (T,Int) => F) = {
    var list = List[F] ()
    var i = firstEmptyIndex
    while (i > 0) {
      i -= 1
      if (firstDeletedIndex < 0 || !isUnoccupied (i))
        list = f(array(i),i) :: list
    }
    list
  }
}


/** */
private final object FixedHashSet {

  final object ResizeNeeded extends Exception

  final val initialBits = 2 // 4 elements

  final val INT_END_OF_LIST = 0x40000000
  final val INT_AVAILABLE_BITS = 0x3FFFFFFF
  final val SHORT_AVAILABLE_BITS = 0x7FFF
  final val BYTE_AVAILABLE_BITS = 0x7F

  final val EMPTY_HASH_SET = EmptyHashSet

  /** Create new array to hold set or map elements. */
  final def newArray[V: ClassManifest] (size: Int): Array[V] =
    if (size <= 0) null else new Array[V] (size)

  /** FixedHashSet implementation with byte-size index arrays. */
  final class ByteHashSet[T: ClassManifest] (bits: Int, a: Array[T], loadFactor: Float)
  extends FixedHashSet[T] (bits, a, loadFactor) with Serializable {

    private[this] var indexTable = new Array[Byte] (2 << bits)
    final def getIndexArray = indexTable

    // make -1 default instead of 0
    protected final def firstIndex (i: Int) = ~indexTable(i)
    protected final def nextIndex (i: Int) = ~indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) =
    indexTable(i) = (~v).asInstanceOf[Byte]
    protected final def setNextIndex (i: Int, v: Int) =
      indexTable(len+i) = (~v).asInstanceOf[Byte]

    override def cloneData {
      super.cloneData
      indexTable = java.util.Arrays.copyOf (indexTable, 2 << bits)
      localArray = getArray
    }

    final override def clear {
      super.clear
      java.util.Arrays.fill (indexTable, 0.asInstanceOf[Byte])
    }

    final def hcBitmask = BYTE_AVAILABLE_BITS ^ (len-1)

    // 'inline' some methods for better performance

    private[this] val len = 1 << bits
    // scalac treat 'array' as method call :-(
    // until it's private[this] so make a local copy
    private[this] var localArray = a
    final override def positionOf[B >: T] (elem: B) = {
      val hc = hash(elem)
      val mask = BYTE_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var i = ~indexTable(hc & (len-1))
      while (i >= 0 && ((hcBits != (i & mask)) || {
        val x = localArray(i & (len-1))
        (x.asInstanceOf[Object] ne elem.asInstanceOf[Object]) && x != elem
      }))
      i = ~indexTable(len + (i & (len-1)))
      if (i >= 0) i & (len-1) else -1
    }

    final def isUnoccupied (i: Int): Boolean = {
      val next = indexTable (len+i)
      next == 0 || next > 1
    }

    final override def rehash (that: FixedHashSet[T]) {
      val array2 = that.getArray
      if (array2 eq null) return
      val len2 = array2.length
      val size2 = that.hashLength
      var i = 0
      //
      java.util.Arrays.fill (indexTable, len, len+size2, 1.asInstanceOf[Byte])
      val mask = BYTE_AVAILABLE_BITS ^ (len-1)
      val mask2 = that.hcBitmask
      if ((mask2 & mask) == mask) {
        val index2 = that.getIndexArray.asInstanceOf[Array[Byte]]
        while (i < size2) {
          var j = ~index2(i)
          var next1: Byte = 0
          var next2: Byte = 0
          while (j >= 0) {
            val arrayIndex = j & (size2-1)
            val hashIndex = i | (j & (mask ^ mask2))
            if (hashIndex == i) {
              if (next1 < 0) indexTable (len + arrayIndex) = next1
              next1 = (~arrayIndex ^ (j & mask)).asInstanceOf[Byte]
            } else {
              if (next2 < 0) indexTable (len + arrayIndex) = next2
              next2 = (~arrayIndex ^ (j & mask)).asInstanceOf[Byte]
            }
            j = ~index2(size2 + arrayIndex)
          }
          if (next1 < 0) indexTable (i) = next1
          if (next2 < 0) indexTable (i + size2) = next2
          i += 1
        }
      } else
        while (i < len2) {
          val e = array2(i)
          // inline addNew
          val hc = hash(e)
          val j = hc & (len - 1)
          val next = indexTable(j)
          indexTable (j) = (~i ^ (hc & mask)).asInstanceOf[Byte]
          if (next < 0) indexTable (len+i) = next
          i += 1
        }
      setSize (len2)
    }
    final override def add (elem: T): Int = {
      val hc = hash(elem)
      val i = hc & (len - 1)
      val next = indexTable (i)
      // Check if elem already present
      val mask = BYTE_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var j = ~next
      while (j >= 0) {
        val k = j & (len - 1)
        if (hcBits == (j & mask)) {
          val o = localArray (k)
          if ((o.asInstanceOf[Object] eq elem.asInstanceOf[Object]) || o == elem)
            return k
        }
        j = ~indexTable (len+k)
      }
      val newIndex = findEmptySpot
      indexTable (i) = (~newIndex ^ hcBits).asInstanceOf[Byte]
      indexTable (len + newIndex) = if (next < 0) next else 1
      localArray (newIndex) = elem
      newIndex
    }
  }

  /** FixedHashSet implementation with short-size index arrays.
   */
  final class ShortHashSet[T:ClassManifest] (bits: Int, a: Array[T], loadFactor: Float)
        extends FixedHashSet[T] (bits, a, loadFactor) with Serializable {
    private[this] var indexTable = new Array[Short] (2 << bits)
    final def getIndexArray = indexTable

    protected final def firstIndex (i: Int) = ~indexTable(i)
    protected final def nextIndex (i: Int) = ~indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) =
      indexTable(i) = (~v).asInstanceOf[Short]
    protected final def setNextIndex (i: Int, v: Int) =
      indexTable(len+i) = (~v).asInstanceOf[Short]

    override def cloneData {
      super.cloneData
      indexTable = java.util.Arrays.copyOf (indexTable, 2 << bits)
      localArray = getArray
    }

    final override def clear {
      super.clear
      java.util.Arrays.fill (indexTable, 0.asInstanceOf[Short])
    }

    final def hcBitmask = SHORT_AVAILABLE_BITS ^ (len-1)

    // 'inline' some methods for better performance

    private[this] val len = 1 << bits
    private[this] var localArray = a
    final override def positionOf[B >: T] (elem: B) = {
      val hc = hash(elem)
      val mask = SHORT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var i = ~indexTable(hc & (len-1))
      while (i >= 0 && ((hcBits != (i & mask)) || {
        val x = localArray(i & (len-1))
        (x.asInstanceOf[Object] ne elem.asInstanceOf[Object]) && x != elem
      }))
      i = ~indexTable(len + (i & (len-1)))
      if (i >= 0) i & (len-1) else -1
    }
    final def isUnoccupied (i: Int): Boolean = {
      val next = indexTable(len+i)
      next == 0 || next > 1
    }
    final override def rehash (that: FixedHashSet[T]) {
      val array2 = that.getArray
      if (array2 eq null) return
      val len2 = array2.length
      val size2 = that.hashLength
      var i = 0
      //
      java.util.Arrays.fill (indexTable, len, len+size2, 1.asInstanceOf[Short])
      val mask = SHORT_AVAILABLE_BITS ^ (len-1)
      val mask2 = that.hcBitmask
      if ((mask2 & mask) == mask) {
        val index2 = that.getIndexArray.asInstanceOf[Array[Short]]
        while (i < size2) {
          var j = ~index2(i)
          var next1: Short = 0
          var next2: Short = 0
          while (j >= 0) {
            val arrayIndex = j & (size2-1)
            val hashIndex = i | (j & (mask ^ mask2))
            if (hashIndex == i) {
              if (next1 < 0) indexTable (len + arrayIndex) = next1
              next1 = (~arrayIndex ^ (j & mask)).asInstanceOf[Short]
            } else {
              if (next2 < 0) indexTable (len + arrayIndex) = next2
              next2 = (~arrayIndex ^ (j & mask)).asInstanceOf[Short]
            }
            j = ~index2(size2 + arrayIndex)
          }
          if (next1 < 0) indexTable (i) = next1
          if (next2 < 0) indexTable (i + size2) = next2
          i += 1
        }
      } else
        while (i < len2) {
          val e = array2(i)
          // inline addNew
          val hc = hash(e)
          val j = hc & (len - 1)
          val next = indexTable(j)
          indexTable (j) = (~i ^ (hc & mask)).asInstanceOf[Short]
          if (next < 0) indexTable (len+i) = next
          i += 1
        }
      setSize (len2)
    }
    final override def add (elem: T): Int = {
      val hc = hash(elem)
      val i = hc & (len - 1)
      val next = indexTable (i)
      // Check if elem already present
      val mask = SHORT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var j = ~next
      while (j >= 0) {
        val k = j & (len - 1)
        if (hcBits == (j & mask)) {
          val o = localArray (k)
          if ((o.asInstanceOf[Object] eq elem.asInstanceOf[Object]) || o == elem)
            return k
        }
        j = ~indexTable (len+k)
      }
      val newIndex = findEmptySpot
      indexTable (i) = (~newIndex ^ hcBits).asInstanceOf[Short]
      indexTable (len + newIndex) = if (next < 0) next else 1
      localArray (newIndex) = elem
      newIndex
    }
  }

  /** FixedHashSet implementation with int-size index arrays. */
  final class IntHashSet[T: ClassManifest] (bits: Int, a: Array[T], loadFactor: Float) extends FixedHashSet[T] (bits, a, loadFactor)
  with Serializable {

    private[this] var indexTable = new Array[Int] (2 << bits)

    final def getIndexArray = indexTable

    protected final def firstIndex (i: Int)            = ~indexTable(i)
    protected final def nextIndex (i: Int)             = ~indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) = indexTable(i) = ~v
    protected final def setNextIndex (i: Int, v: Int)  = indexTable(len+i) = ~v

    override def cloneData {
      super.cloneData
      indexTable = java.util.Arrays.copyOf (indexTable, 2 << bits)
      localArray = getArray
    }

    final override def clear {
      super.clear
      java.util.Arrays.fill (indexTable, 0)
    }

    final def hcBitmask = INT_AVAILABLE_BITS ^ (len-1)
    final override def eolBitmask = INT_END_OF_LIST
    final override def deletedEOL = ~INT_END_OF_LIST

    // 'inline' some methods for better performance

    private[this] val len = 1 << bits
    private[this] var localArray = a
    final override def positionOf[B >: T] (elem: B): Int = {
      val hc = hash (elem)
      val mask = INT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var prev = -1
      var curr = hc & (len-1)
      var i = ~indexTable(curr)
      while (i >= 0) {
        prev = curr
        curr = i & (len-1)
        if (hcBits == (i & mask)) {
          val x = localArray(curr)
          if ((x.asInstanceOf[Object] eq elem.asInstanceOf[Object]) || x == elem)
            return curr
        }
        if ((i & INT_END_OF_LIST) != 0) return -1
        curr += len
        i = ~indexTable(curr)
      }
      -1
    }

    final override def positionOfInt (elem: Int): Int = {
      if (localArray.isInstanceOf[Array[Int]]) {
        val unboxedArray: Array[Int] = localArray.asInstanceOf[Array[Int]]
        val hc = hash (elem)
        val mask = INT_AVAILABLE_BITS ^ (len-1)
        val hcBits = hc & mask
        var prev = -1
        var curr = hc & (len-1)
        var i = ~indexTable(curr)
        while (i >= 0) {
          prev = curr
          curr = i & (len-1)
          if (hcBits == (i & mask)) {
            val x = unboxedArray(curr)
            if (x == elem) return curr
          }
          if ((i & INT_END_OF_LIST) != 0) return -1
          curr += len
          i = ~indexTable(curr)
        }
        -1
      } else positionOf (elem)
    }

    final def isUnoccupied (i: Int): Boolean = indexTable (len+i) > 1

    final override def rehash (that: FixedHashSet[T]) {
      val array2 = that.getArray
      if (array2 eq null) return
      val len2 = array2.length
      val size2 = that.hashLength
      var i = 0
      //
      val mask = INT_AVAILABLE_BITS ^ (len-1)
      val mask2 = that.hcBitmask
      if ((mask2 & mask) == mask) {
        val index2 = that.getIndexArray.asInstanceOf[Array[Int]]
        while (i < size2) {
          var j = ~index2(i)
          var next1 = 0
          var next2 = 0
          while (j >= 0) {
            val arrayIndex = j & (size2-1)
            val hashIndex = i | (j & (mask ^ mask2))
            if (hashIndex == i) {
              if (next1 < 0) indexTable (len + arrayIndex) = next1
              next1 = ~(arrayIndex | (j & mask) | (if (next1 < 0) 0 else INT_END_OF_LIST))
            } else {
              if (next2 < 0) indexTable (len + arrayIndex) = next2
              next2 = ~(arrayIndex | (j & mask) | (if (next2 < 0) 0 else INT_END_OF_LIST))
            }
            j = if ((j & INT_END_OF_LIST) != 0) -1 else ~index2(size2 + arrayIndex)
          }
          if (next1 < 0) indexTable (i) = next1
          if (next2 < 0) indexTable (i + size2) = next2
          i += 1
        }
      } else
        while (i < len2) {
          val e = array2(i)
          // inline addNew
          val hc = hash(e)
          val j = hc & (len - 1)
          val next = indexTable(j)
          indexTable (j) = ~(i | (hc & mask) | (if (next < 0) 0 else INT_END_OF_LIST))
          if (next < 0) indexTable (len+i) = next
          i += 1
        }
      setSize (len2)
    }

    final override def add (elem: T): Int = {
      val hc = hash(elem)
      val i = hc & (len - 1)
      val next = indexTable (i)
      // Check if elem already present
      val mask = INT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var j = ~next
      while (j >= 0) {
        val k = j & (len - 1)
        if (hcBits == (j & mask)) {
          val o = localArray (k)
          if ((o.asInstanceOf[Object] eq elem.asInstanceOf[Object]) || o == elem)
            return k
        }
        j = if ((j & INT_END_OF_LIST) != 0) -1 else ~indexTable (len+k)
      }
      val newIndex = findEmptySpot
      indexTable (i) = ~(newIndex | hcBits | (if (next < 0) 0 else INT_END_OF_LIST))
      if (next < 0) indexTable (len + newIndex) = next
      localArray (newIndex) = elem
      newIndex
    }

    final override def addInt (elem: Int): Int = {
      if (localArray.isInstanceOf[Array[Int]]) {
        val unboxedArray: Array[Int] = localArray.asInstanceOf[Array[Int]]
        val hc = hash(elem)
        val i = hc & (len - 1)
        val next = indexTable (i)
        // Check if elem already present
        val mask = INT_AVAILABLE_BITS ^ (len-1)
        val hcBits = hc & mask
        var j = ~next
        while (j >= 0) {
          val k = j & (len - 1)
          if (hcBits == (j & mask)) {
            val o = unboxedArray (k)
            if (o == elem) return k
          }
          j = if ((j & INT_END_OF_LIST) != 0) -1 else ~indexTable (len+k)
        }
        val newIndex = findEmptySpot
        indexTable (i) = ~(newIndex | hcBits | (if (next < 0) 0 else INT_END_OF_LIST))
        if (next < 0) indexTable (len + newIndex) = next
        unboxedArray (newIndex) = elem
        newIndex
      } else {
        add (elem.asInstanceOf[T])
      }
    }
  }

  /** FixedHashSet implementation with int-size index arrays
   * and unboxed array of Objects.
   */
  final class IntObjectHashSet[T: ClassManifest] (bits: Int, a: Array[T], loadFactor: Float) extends FixedHashSet[T] (bits, a, loadFactor)
  with Serializable {

    private[this] var indexTable = new Array[Int] (2 << bits)
    final def getIndexArray = indexTable

    protected final def firstIndex (i: Int) = ~indexTable(i)
    protected final def nextIndex (i: Int) = ~indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) = indexTable(i) = ~v
    protected final def setNextIndex (i: Int, v: Int) = indexTable(len+i) = ~v

    override def cloneData {
      super.cloneData
      indexTable = java.util.Arrays.copyOf (indexTable, 2 << bits)
      localArray = getArray.asInstanceOf[Array[Object]]
    }
    final override def clear {
      super.clear
      java.util.Arrays.fill (indexTable, 0)
    }
    final def hcBitmask = INT_AVAILABLE_BITS ^ (len-1)
    final override def eolBitmask = INT_END_OF_LIST
    final override def deletedEOL = ~INT_END_OF_LIST

    // 'inline' some methods for better performance

    private[this] val len = 1 << bits
    private[this] var localArray: Array[Object] = a.asInstanceOf[Array[Object]]
    final override def positionOf[B >: T] (elem: B): Int = {
      val hc = hash(elem)
      val mask = INT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var prev = -1
      var curr = hc & (len-1)
      var i = ~indexTable(curr)
      while (i >= 0) {
        prev = curr
        curr = i & (len-1)
        if (hcBits == (i & mask)) {
          val x = localArray(curr)
          if ((x eq elem.asInstanceOf[Object]) || (x ne null) && (x equals elem))
            return curr
        }
        if ((i & INT_END_OF_LIST) != 0) return -1
        curr += len
        i = ~indexTable(curr)
      }
      -1
    }

    final def isUnoccupied (i: Int): Boolean = indexTable (len+i) > 1

    final override def rehash (that: FixedHashSet[T]) {
      val array2 = that.getArray
      if (array2 eq null) return
      val len2 = array2.length
      val size2 = that.hashLength
      var i = 0
      //
      val mask = INT_AVAILABLE_BITS ^ (len-1)
      val mask2 = that.hcBitmask
      if ((mask2 & mask) == mask) {
        val index2 = that.getIndexArray.asInstanceOf[Array[Int]]
        while (i < size2) {
          var j = ~index2(i)
          var next1 = 0
          var next2 = 0
          while (j >= 0) {
            val arrayIndex = j & (size2-1)
            val hashIndex = i | (j & (mask ^ mask2))
            if (hashIndex == i) {
              if (next1 < 0) indexTable (len + arrayIndex) = next1
              next1 = ~(arrayIndex | (j & mask) | (if (next1 < 0) 0 else INT_END_OF_LIST))
            } else {
              if (next2 < 0) indexTable (len + arrayIndex) = next2
              next2 = ~(arrayIndex | (j & mask) | (if (next2 < 0) 0 else INT_END_OF_LIST))
            }
            j = if ((j & INT_END_OF_LIST) != 0) -1 else ~index2(size2 + arrayIndex)
          }
          if (next1 < 0) indexTable (i) = next1
          if (next2 < 0) indexTable (i + size2) = next2
          i += 1
        }
      } else
        while (i < len2) {
          val e = array2(i)
          // inline addNew
          val hc = hash(e)
          val j = hc & (len - 1)
          val next = indexTable(j)
          indexTable (j) = ~(i | (hc & mask) | (if (next < 0) 0 else INT_END_OF_LIST))
          if (next < 0) indexTable (len+i) = next
          i += 1
        }
      setSize (len2)
    }
    final override def add (elem: T): Int = {
      val hc = hash(elem)
      val i = hc & (len - 1)
      val next = indexTable (i)
      // Check if elem already present
      val mask = INT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var j = ~next
      while (j >= 0) {
        val k = j & (len - 1)
        if (hcBits == (j & mask)) {
          val o = localArray (k)
          if ((o eq elem.asInstanceOf[Object]) || ((o ne null) && (o equals elem)))
            return k
        }
        j = if ((j & INT_END_OF_LIST) != 0) -1 else ~indexTable (len+k)
      }
      val newIndex = findEmptySpot
      indexTable (i) = ~(newIndex | hcBits | (if (next < 0) 0 else INT_END_OF_LIST))
      if (next < 0) indexTable (len + newIndex) = next
      localArray (newIndex) = elem.asInstanceOf[Object]
      newIndex
    }
  }


  /** Empty FixedHashSet implementation.
   */
  final object EmptyHashSet extends FixedHashSet[Any] (initialBits - 1, null, DEFAULT_LOAD_FACTOR) with Serializable {
    final override def positionOf[B >: Any] (elem: B) = -1
    final def isUnoccupied (i: Int): Boolean = true
    protected final def firstIndex (i: Int) = -1
    protected final def nextIndex (i: Int) = -1
    protected final def setFirstIndex (i: Int, v: Int) { }
    protected final def setNextIndex (i: Int, v: Int) { }
    final def hcBitmask = 0
    final def getIndexArray = null
    final def rehash (that: FixedHashSet[Any]) { }
    final override def add (elem: Any): Int = { throw ResizeNeeded }
  }

  /** Construct FixedHashSet implementation with given parameters.
   */
  final def apply[T: ClassManifest] (bits: Int): FixedHashSet[T] =
    apply (bits, DEFAULT_LOAD_FACTOR)

  /** Construct FixedHashSet implementation with given parameters.
   */
  final def apply[T: ClassManifest] (bits: Int, loadFactor: Float): FixedHashSet[T] =
    if (bits <= 0)
      EMPTY_HASH_SET.asInstanceOf[FixedHashSet[T]]
    else {
      val hashSize = 1 << bits
      val dataSize = (hashSize * loadFactor).asInstanceOf[Int]
      if (dataSize < 1 || dataSize > hashSize)
        throw new IllegalArgumentException ("Illegal load factor: " + loadFactor)
      val a = newArray[T] (dataSize)
      if (bits < 8 && dataSize == hashSize)
        new ByteHashSet (bits, a, loadFactor) else
          if (bits < 16 && dataSize == hashSize)
            new ShortHashSet (bits, a, loadFactor) else
              if (a.isInstanceOf[Array[Object]])
                new IntObjectHashSet (bits, a, loadFactor)
              else
                new IntHashSet (bits, a, loadFactor)
    }

  /** Construct FixedHashSet implementation with capacity (bits)
   *  and copy values from another set.
   */
  final def apply[T: ClassManifest] (bits: Int, that: FixedHashSet[T]): FixedHashSet[T] = {
    val hashSize = 1 << bits
    val dataSize = (hashSize * that.loadFactor).asInstanceOf[Int]
    if (dataSize < 1 || dataSize > hashSize)
      throw new IllegalArgumentException ("Illegal load factor: " + that.loadFactor)
    val a = resizeArray (that.getArray, dataSize)
    val newSet = if (bits < 8 && dataSize == hashSize)
      new ByteHashSet (bits, a, that.loadFactor) else
        if (bits < 16 && dataSize == hashSize)
          new ShortHashSet (bits, a, that.loadFactor) else
            if (a.isInstanceOf[Array[Object]])
              new IntObjectHashSet (bits, a, that.loadFactor)
            else
              new IntHashSet (bits, a, that.loadFactor)
    newSet.rehash (that)
    newSet
  }

  /** Create a new boxed array and copy elements from another array. */
  final def resizeArray[T: ClassManifest] (a: Array[T], newSize: Int): Array[T] = {
    val newArray = new Array[T] (newSize)
    if (a ne null)
      Array.copy (a, 0, newArray, 0, a.length)
    newArray
  }

  /** Filtering callbacks. */
  trait Filter[T] {
    def check (key: T, index: Int): Boolean
    def create (size: Int): Unit
    def copy (i: Int, j: Int): Unit
  }

  /** Improve hashcode. */
  final def hash(h: Int): Int = {
    // This function ensures that hashCodes that differ only by
    // constant multiples at each bit position have a bounded
    // number of collisions (approximately 8 at default load factor).
    val h2 = h ^ (h >>> 20) ^ (h >>> 12)
    h2 ^ (h2 >>> 7) ^ (h2 >>> 4)
  }

  final def hash(o: Any): Int =
    if (o.asInstanceOf[Object] eq null) 0 else hash(o.hashCode)

  /** The load factor used when none specified in constructor. */
  final val DEFAULT_LOAD_FACTOR = 1f
}