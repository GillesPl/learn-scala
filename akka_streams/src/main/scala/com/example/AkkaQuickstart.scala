package com.example

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.typed.scaladsl.ActorMaterializer

object ActorSourceSinkExample {

  //todo create een actor system als voorbeel, misschien in PLAY
  val system: ActorSystem[_] = ???

  // de materializer die gaat van een flow een running stream maken
  // in ons geval word system een running stream
  // An ActorMaterializer takes a stream blueprint and turns it into a running stream. todo is de definitie
  implicit val mat: ActorMaterializer = ActorMaterializer()(system)

  {
    // #actor-source-ref
    import akka.actor.typed.ActorRef
    import akka.stream.OverflowStrategy
    import akka.stream.scaladsl.{ Sink, Source }
    import akka.stream.typed.scaladsl.ActorSource

    trait Protocol
    case class Message(msg: String) extends Protocol
    case object Complete extends Protocol
    case class Fail(ex: Exception) extends Protocol

    // een source is een actor die enkel berichten ontvangt en de sink gaat dan de berichten doorsturen naar een andere actor, met of zonder backpressure
    // accepteert alleen messages met zelfde type als stream ==> protocol
    // actor source is een collectie van sources gericht naar integreren van typed actors
    val source: Source[Protocol, ActorRef[Protocol]] = ActorSource.actorRef[Protocol](
      completionMatcher = {
        case Complete ⇒
      },
      failureMatcher = {
        case Fail(ex) ⇒ ex
      },
      bufferSize = 8,
      overflowStrategy = OverflowStrategy.fail
    )

    val ref = source.collect {
      case Message(msg) ⇒ msg
    }.to(Sink.foreach(println)).run()

    ref ! Message("msg1")
    // ref ! "msg2" Does not compile
    // #actor-source-ref
  }

  // actor sink is een collectie van sinks gericht naar integreren met typed actors
  // de sink gaat een stream van messages doorsturen naar de Actor, zonder backpressure dus gaat een overflow
  {
    // #actor-sink-ref
    import akka.actor.typed.ActorRef
    import akka.stream.scaladsl.{ Sink, Source }
    import akka.stream.typed.scaladsl.ActorSink

    trait Protocol
    case class Message(msg: String) extends Protocol
    case object Complete extends Protocol
    case class Fail(ex: Throwable) extends Protocol

    val actor: ActorRef[Protocol] = ???

    val sink: Sink[Protocol, NotUsed] = ActorSink.actorRef[Protocol](
      ref = actor,
      onCompleteMessage = Complete,
      onFailureMessage = Fail.apply
    )

    Source.single(Message("msg1")).runWith(sink)
    // #actor-sink-ref
  }


  // de ack geeft aan wanneer de actor terug vrij is voor messages te ontvangen ==> backpressure
  // actor sink is een collectie van sinks gericht naar integreren met typed actors
  // de sink gaat een stream van messages doorsturen naar de Actor, die gaat een backpressure signal terug sturen
  {
    // #actor-sink-ref-with-ack
    import akka.actor.typed.ActorRef
    import akka.stream.scaladsl.{ Sink, Source }
    import akka.stream.typed.scaladsl.ActorSink

    trait Ack
    object Ack extends Ack

    trait Protocol
    case class Init(ackTo: ActorRef[Ack]) extends Protocol
    case class Message(ackTo: ActorRef[Ack], msg: String) extends Protocol
    case object Complete extends Protocol
    case class Fail(ex: Throwable) extends Protocol

    val actor: ActorRef[Protocol] = ???

    val sink: Sink[String, NotUsed] = ActorSink.actorRefWithAck(
      ref = actor,
      onCompleteMessage = Complete,
      onFailureMessage = Fail.apply,
      messageAdapter = Message.apply,
      onInitMessage = Init.apply,
      ackMessage = Ack
    )

    Source.single("msg1").runWith(sink)
    // #actor-sink-ref-with-ack
  }
}


//todo source is de producer
//todo alle stappen ertussen zijn flows (source en sink zijn op zich ook flows)
//todo sink is de consumer

//todo geheel is een graph