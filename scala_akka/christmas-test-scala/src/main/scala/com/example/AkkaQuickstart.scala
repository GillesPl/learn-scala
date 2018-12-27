//#full-example
package com.example

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }

// Greeter actor companion object
object Greeter {
  // props is the method to create the actor, the Props return value is a configuration class to specify the options of an actor
  def props(message: String, printerActor: ActorRef): Props = Props(new Greeter(message, printerActor))

  // These are the messages that the greeter can receive and process
  // TODO: question: what is the difference between the final case class and the case object?
  final case class WhoToGreet(who: String)
  case object Greet
}


// Greeter actor implementation, note the extends Actor, the message can be the initial state of the actor
class Greeter(message: String, printerActor: ActorRef) extends Actor {
  import Greeter._
  import Printer._

  // Note that the state is mutable
  var greeting = ""

  // The receive methods defines behavior of the Actor
  def receive: PartialFunction[Any, Unit] = {
    // this is an event that triggers a state change
    case WhoToGreet(who) =>
      greeting = message + ", " + who

    // this is an event that triggers the printeractor to do a greeting with the state(greeting) of the greeter
    case Greet =>
      printerActor ! Greeting(greeting)
  }
}


object Printer {

  def props: Props = Props[Printer]
  final case class Greeting(greeting: String)
}

// this is also an actor with an axtra trait that enables logging
class Printer extends Actor with ActorLogging {
  import Printer._

  def receive = {
    // this is the greeting method, the actor will listen on events and when he gets a greeting event he will trigger the greeting
    case Greeting(greeting) =>
      log.info("Greeting received (from " + sender() + "): " + greeting)
  }
}


//#main-class
object AkkaQuickstart extends App {
  import Greeter._

  // Create the 'helloAkka' actor system, this is the parent actor
  val system: ActorSystem = ActorSystem("helloAkka")

  // Create the printer actor
  val printer: ActorRef = system.actorOf(Printer.props, "printerActor")

  // Create the 'greeter' actors based on the Greeter Actore we created on line 19
  val howdyGreeter: ActorRef =
    system.actorOf(Greeter.props("Howdy", printer), "howdyGreeter")
  val helloGreeter: ActorRef =
    system.actorOf(Greeter.props("Hello", printer), "helloGreeter")
  val goodDayGreeter: ActorRef =
    system.actorOf(Greeter.props("Good day", printer), "goodDayGreeter")

  // Send a message to the HowdyGreeter actor with a message "Akka" which will change the state of that actor
  // TODO the exlamation marks mean to put a message in the mailbox of the actor AKA Bang
  howdyGreeter ! WhoToGreet("Akka")
  // send a message that the howdy greeter has to execute the greet command
  howdyGreeter ! Greet

  howdyGreeter ! WhoToGreet("Lightbend")
  howdyGreeter ! Greet

  helloGreeter ! WhoToGreet("Scala")
  helloGreeter ! Greet

  goodDayGreeter ! WhoToGreet("Play")
  goodDayGreeter ! Greet

}