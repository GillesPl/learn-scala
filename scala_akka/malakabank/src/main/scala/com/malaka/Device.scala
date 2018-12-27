package com.malaka

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

//contains the current temperature or,
//indicates that a temperature is not yet available.
object Device {
  // defines how to construct the behavior for the device actor
  def apply(groupId: String, deviceId: String): Behavior[DeviceMessage] =
    Behaviors.setup(context ⇒ new Device(context, groupId, deviceId))

  sealed trait DeviceMessage

  // Read protocol
  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends DeviceMessage
  // is part of the reply for the actor ref => ack
  final case class RespondTemperature(requestId: Long, value: Option[Double])

  //  Write protocol
  final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded]) extends DeviceMessage
  // ack message for record temperature
  final case class TemperatureRecorded(requestId: Long)
}

// actor behavior implementation
class Device(context: ActorContext[Device.DeviceMessage], groupId: String, deviceId: String) extends AbstractBehavior[Device.DeviceMessage] {

  import Device._

  //state
  var lastTemperatureReading: Option[Double] = None

  context.log.info("Device actor {}-{} started", groupId, deviceId)

  override def onMessage(msg: DeviceMessage): Behavior[DeviceMessage] = {
    msg match {
      case ReadTemperature(id, replyTo) ⇒
        replyTo ! RespondTemperature(id, lastTemperatureReading) // respond to the actor ref with the temp that was read
        this

      case RecordTemperature(id, value, replyTo) ⇒
        context.log.info("Recorded temperature reading {} with {}", value, id)
        lastTemperatureReading = Some(value)
        replyTo ! TemperatureRecorded(id)
        this

    }
  }

  // handler for actor signals
  // this is a specific watcher if the actor has stopped
  override def onSignal: PartialFunction[Signal, Behavior[DeviceMessage]] = {
    case PostStop ⇒
      context.log.info("Device actor {}-{} stopped", groupId, deviceId)
      this
  }

}