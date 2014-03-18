package org.dbpedia.spotlight.io

import akka.util.ByteString
import akka.actor.{IO, IOManager, Actor}
import org.apache.spark.streaming.receivers.Receiver

class SocketTextStreamReceiver (host:String,
                                port:Int,
                                bytesToString: ByteString => String) extends Actor with Receiver {

    override def preStart = IOManager(context.system).connect(host, port)

    def receive = {
        case IO.Read(socket, bytes) => pushBlock(bytesToString(bytes))
    }

}