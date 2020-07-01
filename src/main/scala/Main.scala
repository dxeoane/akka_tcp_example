import akka.Done
import akka.actor.ActorSystem
import akka.stream.{KillSwitches, UniqueKillSwitch}
import akka.stream.scaladsl.{Flow, Framing, Keep, Sink, Source, Tcp}
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {

  implicit val system: ActorSystem = ActorSystem()

  lazy val (killSwitch: UniqueKillSwitch, server: Future[Done]) =
    Tcp().bind("0.0.0.0", 5555)
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(Sink.foreach(_.handleWith(connectionHandler)))(Keep.both)
      .run

  val welcome = Source.single(
    """
      |Echo server ...
      |Type "quit" to close this connection
      |Type "quit!" to shutdown the server
      |""".stripMargin)

  val parser = Flow[String].map{ message =>
    // Close server
    if (message == "quit!") killSwitch.shutdown()
    message
  }.takeWhile(message =>  (message != "quit") && (message != "quit!"))

  val connectionHandler = Flow[ByteString]
    .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
    .map(_.utf8String.trim)
    .via(parser)
    .merge(welcome)
    .map(_ + "\n")
    .map(ByteString(_))

  implicit val ec: ExecutionContext = system.dispatcher
  server.onComplete(
    _ => system.terminate()
  )


}
