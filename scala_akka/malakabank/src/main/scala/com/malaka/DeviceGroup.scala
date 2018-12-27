package com.malaka

import scala.concurrent.duration._
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object DeviceGroup {
  def apply(groupId: String): Behavior[DeviceGroupMessage] =
    Behaviors.setup(context ⇒ new DeviceGroup(context, groupId))

  trait DeviceGroupMessage

  private final case class DeviceTerminated(device: ActorRef[Device.DeviceMessage], groupId: String, deviceId: String) extends DeviceGroupMessage

}

//#query-added
class DeviceGroup(context: ActorContext[DeviceGroup.DeviceGroupMessage], groupId: String)
  extends AbstractBehavior[DeviceGroup.DeviceGroupMessage] {
  import DeviceGroup._
  import DeviceManager._

  //state
  private var deviceIdToActor = Map.empty[String, ActorRef[Device.DeviceMessage]]

  context.log.info("DeviceGroup {} started", groupId)

  // behavior implementation
  override def onMessage(msg: DeviceGroupMessage): Behavior[DeviceGroupMessage] =
    msg match {
      //#query-added

        // request om een device te tracken in die specifieke groep als er nog geen actor voor die device is gaat hij een aanmaken
      case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) ⇒
        deviceIdToActor.get(deviceId) match {
          case Some(deviceActor) ⇒
            replyTo ! DeviceRegistered(deviceActor)
          case None ⇒
            context.log.info("Creating device actor for {}", trackMsg.deviceId)
            val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")
            context.watchWith(deviceActor, DeviceTerminated(deviceActor, groupId, deviceId))
            //#device-group-register
            deviceIdToActor += deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      case RequestTrackDevice(gId, _, _) ⇒
        context.log.warning(
          "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
          gId, groupId
        )
        this


      // message die de hele device list opvraagt voor een groep
      case RequestDeviceList(requestId, gId, replyTo) ⇒
        if (gId == groupId) {
          replyTo ! ReplyDeviceList(requestId, deviceIdToActor.keySet)
          this
        } else
          Behaviors.unhandled
      //#device-group-remove

        // wanneer een device terminated is word die uit de state gehaald van de groep
      case DeviceTerminated(_, _, deviceId) ⇒
        context.log.info("Device actor for {} has been terminated", deviceId)
        deviceIdToActor -= deviceId
        this

      //#query-added
      // ... other cases omitted

        // vraag alle tempeeraturen op van een groep adhv een devicegroepquery
      case RequestAllTemperatures(requestId, gId, replyTo) ⇒
        if (gId == groupId) {
          context.spawnAnonymous(DeviceGroupQuery(
            deviceIdToActor,
            requestId = requestId,
            requester = replyTo,
            3.seconds
          ))
          this
        } else
          Behaviors.unhandled
    }

  override def onSignal: PartialFunction[Signal, Behavior[DeviceGroupMessage]] = {
    case PostStop ⇒
      context.log.info("DeviceGroup {} stopped", groupId)
      this
  }
}
//#query-added