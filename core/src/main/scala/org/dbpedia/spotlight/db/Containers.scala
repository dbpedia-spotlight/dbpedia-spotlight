package org.dbpedia.spotlight.db

import java.io.{ObjectInput, ObjectOutput, Externalizable}


/**
 * @author Joachim Daiber
 *
 *
 *
 */

object Containers {

  class SFContainer extends Externalizable {
    def this(id: Int, support: Int) {
      this()
      this.id = id; this.support = support
    }

    var id: Int = 0
    var support: Int = 0
    def readExternal(in: ObjectInput) {
      id = in.readInt()
      support = in.readInt()
    }
    def writeExternal(out: ObjectOutput) {
      out.writeInt(id)
      out.writeInt(support)
    }
  }

}
