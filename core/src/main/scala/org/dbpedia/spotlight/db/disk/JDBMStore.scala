package org.dbpedia.spotlight.db.disk

import net.kotek.jdbm.DBMaker

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class JDBMStore[A, B](databaseFile: String) {

  val db = DBMaker.openFile(databaseFile).enableHardCache().make()
  var data = db.getHashMap[A, B]("data")

  def create() {
    data = db.createHashMap[A, B]("data")
  }

  def add(a: A, b: B) {
    data.put(a, b)
  }

  def commit() {
    db.commit()
  }

  def get(key: A): B = data.get(key)

}


