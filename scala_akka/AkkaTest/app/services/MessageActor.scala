package services

import akka.actor.{Actor, Props}

object MessageActor {
  def props = Props[MessageActor]

  case class addMessage(name: String, message: String)
}

class MessageActor extends Actor {
  import MessageActor._

  def receive = {
    case addMessage(name: String, message: String) => {

    }
  }
}