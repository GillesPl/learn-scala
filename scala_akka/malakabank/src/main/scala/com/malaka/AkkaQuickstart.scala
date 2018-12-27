//#full-example
package com.malaka

import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.malaka.Device.DeviceMessage

object IotApp {

  def main(args: Array[String]): Unit = {
    import Device._
    // Create ActorSystem and top level supervisor
//    val system = ActorSystem[Nothing](IotSupervisor(), "iot-system")
//
//
//    // create 3 devices
//    val device1 = ActorSystem[DeviceMessage](Device("test", "1"), "Device_1")
//    val device2 = ActorSystem[DeviceMessage](Device("test", "2"), "Device_2")
//    val device3 = ActorSystem[DeviceMessage](Device("test", "3"), "Device_3")
  }

}