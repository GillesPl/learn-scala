//#full-example
package com.malaka

import akka.actor.typed.ActorSystem

object IotApp {

  def main(args: Array[String]): Unit = {
    // Create ActorSystem and top level supervisor
    val system = ActorSystem[Nothing](IotSupervisor(), "iot-system")

    // TODO look how to create several devices and create some sort of test
    //val device = system.systemActorOf(Device.apply("sdf", "123")


  }

}