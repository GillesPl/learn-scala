package com.example

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import com.example.firstStepsEmployees.system

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration


object ReactiveExample extends App {

  final case class Worker(name: String)

  final case class JobDescription(name: String, proffesion: String)

  final case class Employee(worker: Worker, startedOn: Long, description: String) {
    def getDevelopers: Set[JobDescription] = description.split(" ").collect {
      case t if t.compareToIgnoreCase("developer") == 0 ⇒ JobDescription(worker.name, description)
    }.toSet
  }

  //Source[Out,M1]
  // dit zou moeten de input zijn van employees, niet predefined maar komt binnen door acties van een gebruiker of meerdere gebruikers bijvoorbeeld.
  val employees: Source[Employee, NotUsed] = Source(
    Employee(Worker("Michallis"), System.currentTimeMillis, "Architect and CTO and CEO and TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Guillaume"), System.currentTimeMillis, "Medior Developer for Trust1Team") ::
      Employee(Worker("Gilles"), System.currentTimeMillis, "Junior Developer for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "Senior Developer for Trust1Team") ::
      Employee(Worker("Michallis"), System.currentTimeMillis, "Architect and CTO and CEO and TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Guillaume"), System.currentTimeMillis, "Medior Developer for Trust1Team") ::
      Employee(Worker("Gilles"), System.currentTimeMillis, "Junior Developer for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "Senior Developer for Trust1Team") ::
      Employee(Worker("Michallis"), System.currentTimeMillis, "Architect and CTO and CEO and TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Guillaume"), System.currentTimeMillis, "Medior Developer for Trust1Team") ::
      Employee(Worker("Gilles"), System.currentTimeMillis, "Junior Developer for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "Senior Developer for Trust1Team") ::
      Employee(Worker("Michallis"), System.currentTimeMillis, "Architect and CTO and CEO and TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Guillaume"), System.currentTimeMillis, "Medior Developer for Trust1Team") ::
      Employee(Worker("Gilles"), System.currentTimeMillis, "Junior Developer for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "Senior Developer for Trust1Team") ::
      Employee(Worker("Michallis"), System.currentTimeMillis, "Architect and CTO and CEO and TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Guillaume"), System.currentTimeMillis, "Medior Developer for Trust1Team") ::
      Employee(Worker("Gilles"), System.currentTimeMillis, "Junior Developer for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "Senior Developer for Trust1Team") ::
      Employee(Worker("Michallis"), System.currentTimeMillis, "Architect and CTO and CEO and TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Guillaume"), System.currentTimeMillis, "Medior Developer for Trust1Team") ::
      Employee(Worker("Gilles"), System.currentTimeMillis, "Junior Developer for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "Senior Developer for Trust1Team") ::
      Employee(Worker("Michallis"), System.currentTimeMillis, "Architect and CTO and CEO and TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Guillaume"), System.currentTimeMillis, "Medior Developer for Trust1Team") ::
      Employee(Worker("Gilles"), System.currentTimeMillis, "Junior Developer for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "Senior Developer for Trust1Team") ::
      Employee(Worker("Michallis"), System.currentTimeMillis, "Architect and CTO and CEO and TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Guillaume"), System.currentTimeMillis, "Medior Developer for Trust1Team") ::
      Employee(Worker("Gilles"), System.currentTimeMillis, "Junior Developer for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "Senior Developer for Trust1Team") ::
      Nil)

  implicit val system = ActorSystem("super-employees")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  employees
    .buffer(10, OverflowStrategy.backpressure)
    .throttle(10, FiniteDuration(1, TimeUnit.SECONDS))
    .runForeach(println)

  val writeWorker= Sink.foreach(println)
  val writeHashtags = Sink.foreach(println)
  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    // broadcast gaat de source stream in 2 streams opsplitsen en die gaan twee verschillende sinks returnen
    // we gebruiken de impliciete graph builder om via de -> operators de graph op tebouwen
    val bcast = b.add(Broadcast[Employee](2))
    employees ~> bcast.in
    bcast.out(0) ~> Flow[Employee].map(_.worker) ~> writeWorker
    bcast.out(1) ~> Flow[Employee].mapConcat(_.getDevelopers.toList) ~> writeHashtags
    // we returnen een closed shape ==> een volledig gesloten graph / of volledig geconnecteerde graph
    ClosedShape
  })
  g.run()



  // een flow die elke employee naar een nummer 1 omzet
  val count: Flow[Employee, Int, NotUsed] = Flow[Employee].map(_ ⇒ 1)

  // we gaan hier de volledige lijst van counts naar 1 vnalue folden ==> som va alle elementen
  // belangrijk is hier dat de sink een Int value heeft als result en de aux heeft een future int die de de intvalue bevat van alle vorige entries
  val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

  // graph is een blueprint, kan dus meerdere keren gebruikt worden op verschillende datasets van employees
  val counterGraph: RunnableGraph[Future[Int]] =
    employees
      .via(count) // we routeren de employee stream VIA de count flow
      /**
        * Mat type parameters on Source[+Out, +Mat], Flow[-In, +Out, +Mat] and Sink[-In, +Mat].
        * They represent the type of values these processing parts return when materialized.
        * When you chain these together, you can explicitly combine their materialized values.
        * In our example we used the Keep.right predefined function, which tells the implementation to only care about the materialized type of the operator currently appended to the right.
        * The materialized type of sumSink is Future[Int] and because of using Keep.right, the resulting RunnableGraph has also a type parameter of Future[Int].
        */
      .toMat(sumSink)(Keep.right) // be sure to keep the materialised view

  val sum: Future[Int] = counterGraph.run()

  sum.foreach(c ⇒ println(s"Total employees processed: $c"))


}
