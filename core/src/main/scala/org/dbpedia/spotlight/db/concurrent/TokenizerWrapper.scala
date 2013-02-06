package org.dbpedia.spotlight.db.concurrent

import java.io.IOException
import org.dbpedia.spotlight.db.model.Tokenizer
import org.dbpedia.spotlight.model.{Token, Text}
import akka.actor.{OneForOneStrategy, Props, ActorSystem, Actor}
import akka.routing.SmallestMailboxRouter
import akka.actor.SupervisorStrategy.Restart
import akka.dispatch.Await
import akka.util
import akka.util.duration._
import akka.pattern.ask

/**
 * A Wrapper for Tokenizer workers.
 *
 * @author Joachim Daiber
 */

class TokenizerWrapper(val tokenizers: Seq[Tokenizer]) extends Tokenizer {

  var requestTimeout = 60

  val system = ActorSystem()
  val workers = tokenizers.map { case tokenizer:Tokenizer =>
    system.actorOf(Props(new TokenizerActor(tokenizer)))
  }.seq

  def size: Int = tokenizers.size

  val router = system.actorOf(Props[TokenizerActor].withRouter(
    SmallestMailboxRouter(routees = workers).withSupervisorStrategy(
      OneForOneStrategy(maxNrOfRetries = 10) {
        case _: IOException => Restart
      })
  )
  )

  implicit val timeout = util.Timeout(requestTimeout seconds)

  override def tokenizeMaybe(text: Text) {
    val futureResult = router ? TokenizerRequest(text)
    Await.result(futureResult, timeout.duration)
  }

  override def tokenize(text: Text): List[Token] = {
    tokenizeMaybe(text)
    text.featureValue[List[Token]]("tokens").get
  }

  def close() {
    system.shutdown()
  }

}

class TokenizerActor(val tokenizer: Tokenizer) extends Actor {

  def receive = {
    case TokenizerRequest(text) => {
      try {
        sender ! tokenizer.tokenizeMaybe(text)

      } catch {
        case e: NullPointerException => throw new IOException("Could not tokenize.")
      }
    }
  }

}


case class TokenizerRequest(text: Text)
