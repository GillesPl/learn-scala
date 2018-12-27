package views

import akka.actor.{Actor, ActorRef, Props}


object MessageActor {
  def props(messages: Map[String, String]): Props = Props(new MessageActor(messages))
  final case class addMessage(sender: String, msg: String)
}


class MessageActor(messages: Map[String, String]) extends Actor {
  import MessageActor._

  var msgs: Map[String, String] = messages

  def receive = {
    case addMessage(sender: String, msg: String) => {
      msgs += (sender, msg)
    }
  }
}
