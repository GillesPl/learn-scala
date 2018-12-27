package com.malaka

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}


// this is the IOT supervisor
// this will supervise the actors for devices and for the dashboard manager
object IotSupervisor {
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing](context ⇒ new IotSupervisor(context))
}

class IotSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing] {
  context.log.info("IoT Application started")

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    // No need to handle any messages
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop ⇒
      context.log.info("IoT Application stopped")
      this
  }
}