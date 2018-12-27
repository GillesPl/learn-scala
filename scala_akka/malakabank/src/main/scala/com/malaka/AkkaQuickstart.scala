//#full-example
package com.malaka

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.malaka.BankActor._

object BankActor {
  def props(bankName: String, printerActor: ActorRef): Props = Props(new BankActor(bankName, printerActor))
  final case class add(amount: Double)
  final case class subtract(amount: Double)
  final case class payTax(percentage: Double)
  case object overview
}


class BankActor(bankName: String, printerActor: ActorRef) extends Actor {
  import BankActor._
  import Printer._

  var safe: Double = 20000.00

  def receive = {
    case add(amount: Double) => {
      safe = safe + amount
    }
    case subtract(amount: Double) => {
      safe = safe - amount
    }
    case payTax(percentage: Double) => {
      safe = safe - (safe * (percentage / 100))
    }
    case overview => {
      printerActor ! Overview(safe)
    }
  }
}

// printer that handles the logging to the console for now
object Printer {
  def props: Props = Props[Printer]
  final case class Overview(amount: Double)
}

class Printer extends Actor with ActorLogging {
  import Printer._

  def receive = {
    case Overview(amount) =>
      log.info("Bank: " + sender() + " has " + amount + " left.")
  }
}


object AkkaQuickstart extends App {

  // Create the 'helloAkka' actor system
  val system: ActorSystem = ActorSystem("malakaBanks")

  // this actor handles the printing of overviews
  val printer: ActorRef = system.actorOf(Printer.props, "printerActor")

  val trust1teamBank = system.actorOf(BankActor.props("Trust1Team", printer), "Trust1TeamBank")
  val malakaBank: ActorRef = system.actorOf(BankActor.props("Malaka", printer), "MalakaBank")

  BankTransaction.exchange(trust1teamBank,malakaBank, 547.04)
  BankTransaction.payTaxes(trust1teamBank, 95)

}

object BankTransaction {
  def exchange(sender: ActorRef, receiver: ActorRef, amount: Double) = {
    sender ! subtract(amount)
    receiver ! add(amount)

    sender ! overview
    receiver ! overview
  }


  def payTaxes(bank: ActorRef, percentage: Double) = {
      bank ! payTax(percentage)
      bank ! overview
  }
}

