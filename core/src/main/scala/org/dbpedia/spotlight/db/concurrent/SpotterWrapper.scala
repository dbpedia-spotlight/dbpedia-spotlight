package org.dbpedia.spotlight.db.concurrent

import java.io.IOException
import org.dbpedia.spotlight.model.{SurfaceFormOccurrence, Text}
import akka.actor.{OneForOneStrategy, Props, ActorSystem, Actor}
import akka.routing.SmallestMailboxRouter
import akka.actor.SupervisorStrategy.Restart
import org.dbpedia.spotlight.spot.Spotter
import akka.pattern.ask
import akka.util
import scala.concurrent.Await
import java.util.concurrent.TimeUnit

/**
 * A Wrapper for Spotter workers.
 *
 * @author Joachim Daiber
 */

class SpotterWrapper(val spotters: Seq[Spotter]) extends Spotter {

  var requestTimeout = 60

  val system = ActorSystem()
  val workers = spotters.map { spotter: Spotter =>
    system.actorOf(Props(new SpotterActor(spotter)))
  }

  def size: Int = spotters.size

  val router = system.actorOf(Props[SpotterActor].withRouter(
    SmallestMailboxRouter(workers.asInstanceOf).withSupervisorStrategy(
      OneForOneStrategy(maxNrOfRetries = 10) {
        case _: IOException => Restart
      })
  )
  )

  implicit val timeout = util.Timeout(requestTimeout, TimeUnit.SECONDS)

  def extract(text: Text): java.util.List[SurfaceFormOccurrence] = {
    val futureResult = router ? SpotterRequest(text)
    Await.result(futureResult, timeout.duration).asInstanceOf[java.util.List[SurfaceFormOccurrence]]
  }

  def close() {
    system.shutdown()
  }

  def getName: String = "SpotterWrapper[%s]".format(spotters.head.getClass.getSimpleName)

  def setName(name: String) {}
}

class SpotterActor(val spotter: Spotter) extends Actor {

  def receive = {
    case SpotterRequest(text) => {
      try {
        sender ! spotter.extract(text)

      } catch {
        case e: NullPointerException => throw new IOException("Could not tokenize.")
      }
    }
  }

}

case class SpotterRequest(text: Text)
