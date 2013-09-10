package org.dbpedia.spotlight.db.concurrent

import java.io.IOException
import org.dbpedia.spotlight.model.{Token, Text}
import akka.actor.{OneForOneStrategy, Props, ActorSystem, Actor}
import akka.routing.SmallestMailboxRouter
import akka.actor.SupervisorStrategy.Restart
import akka.dispatch.Await
import akka.util
import akka.util.duration._
import akka.pattern.ask
import org.apache.commons.lang.NotImplementedException
import org.dbpedia.spotlight.db.tokenize.BaseTextTokenizer
import org.dbpedia.spotlight.db.model.{StringTokenizer, TextTokenizer}

/**
 * A Wrapper for Tokenizer workers.
 *
 * @author Joachim Daiber
 */

class TokenizerWrapper(val tokenizers: Seq[TextTokenizer]) extends TextTokenizer {

  var requestTimeout = 180

  val system = ActorSystem()
  val workers = tokenizers.map { case tokenizer: TextTokenizer =>
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

  def tokenizeRaw(text: String): Seq[String] = {
    throw new NotImplementedException()
  }

  def close() {
    system.shutdown()
  }

  def getStringTokenizer: StringTokenizer = tokenizers.head.getStringTokenizer

}

class TokenizerActor(val tokenizer: TextTokenizer) extends Actor {

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
