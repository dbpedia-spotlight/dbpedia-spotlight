package org.dbpedia.spotlight.storage

import com.twitter.storehaus.algebra.MergeableStore
import com.twitter.storehaus.memcache.MemcacheStore
import scala.collection.mutable

/**
 * Created by dirkw on 3/17/14.
 */
object StorehausCountStore {

    type StorehausCountStore = MergeableStore[String,Int]
    
    private var countStores = mutable.Map[String,MergeableStore[String,Int]]()

    def cachedOrFromConfig(name:String, host:String,port:Int,typ:String = "memcached") = {
        if(!countStores.contains(name))
                countStores.synchronized {
                    countStores += name -> { typ match {
                    case _ =>
                        val client = MemcacheStore.defaultClient(name, s"$host:$port")

                        MemcacheStore.mergeable[String,Int](client, name)
                    }}
                }

        countStores(name)
    }

}
