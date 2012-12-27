package org.dbpedia.spotlight.db.disk

import net.kotek.jdbm.DBMaker

/**
 * Base class for JDBM disk-based stores.
 *
 * @author Joachim Daiber
 */

class JDBMStore[A, B](databaseFile: String) {

  val db = DBMaker.openFile(databaseFile).enableSoftCache().make()
  var data = Option(db.getHashMap[A, B]("data")) match {
    case None => db.createHashMap[A, B]("data")
    case Some(map) => map
  }

  def add(a: A, b: B) {
    data.put(a, b)
  }

  def commit() {
    db.commit()
  }

  def get(key: A): B = data.get(key)

}


