//#full-example
package com.example

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.example.FirstSteps.{done, system}
import com.example.firstSteps2.{result, system}

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration


// Main method = starting point of application
object FirstSteps extends App {

  // aanmaken van Actor system
  implicit val system = ActorSystem("firstSteps")
  //todo de materializer is een factory method voor streams, dit zorgt ervoor dat de streams "runnen"
  implicit val materializer = ActorMaterializer()


  //todo de source is een beschrijving van wat we willen "runnen"
  // een source die de integers 1 tot 100 gaat emitten
  // source params hebben 2 types
  //      1. de eerste is het type van het element
  //      2. de tweede is dat de source extra informatie kan teruggeven, wanneer er geen geproduceerd word gebruiken we NotUsed
  val source: Source[Int, NotUsed] = Source(1 to 100)

  // source.runforeach is een consumer functie voor die stream
  // runforeach zorgt ervoor dat we de stream doorgeven aan een actor
  // de runforeach returned een future[Done] wanneer de stream klaar is
  val done: Future[Done] = source.runForeach(i ⇒ println(i))(materializer)

  // hier gaan we zeggen als de stream klaar is dan mag hij de actor stoppen
  implicit val ec = system.dispatcher
  done.onComplete(_ ⇒ system.terminate())
}

object firstSteps2 extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)

  // scan gaat eerst een berekening doen over de hele stream beginnen bij het cijfer 1 in dit geval, daarna voert hij een vermenigvuldiging uit
  // scan gaat twee waardes teruggeven als een factorial (initial value en calculated value)
  // deze word dan een stream van factorials
  // todo belangrijk is hier dat dit nog niets van uitvoering is, enkel een blueprint van wat er moet gebeuren wanneer de stream word uitgevoert / gerunt!!!
  val factorials = source.scan(BigInt(1))((acc, next) ⇒ acc * next)

  // hier gaan we de factorials overlopen en de waardes opslaan als een bytestring en dan uiteindelijk aan een file toevoegen
  // todo dit kan je zien als een SINK, de file is de receiver van de stream ==> sink
  // de IOResult geeft aan als de stream al klaar is of niet en hoeveel bytes er geconsumed zijn
  val result: Future[IOResult] =
  factorials
    .map(num ⇒ ByteString(s"$num\n"))
    .runWith(FileIO.toPath(Paths.get("factorials.txt")))

  implicit val ec = system.dispatcher
  result.onComplete(_ ⇒ {
    println("done and terminating")
    system.terminate()
  })
}

object firstStepsEmployees extends App {

  final case class Worker(name: String)

  final case class JobDescription(name: String, proffesion: String)

  final case class Employee(worker: Worker, startedOn: Long, description: String) {
    // function that collects all the proffessions in the desc and returns it as a set of professions
    // this is based on the example just as an easy thing to work with
    def getJobs: Set[JobDescription] = description.split(" ").collect {
      // returnes the proffesions without the @ (@test => test)
      case t if t.startsWith("@") ⇒ JobDescription(worker.name, t)
    }.toSet
  }

  // hier gaan we een source van tweets maken, dit is de blueprint van onze stream
  // later gaan we de stream uitvoeren maar die nog voor en tijdens de flow bewerken
  val employees: Source[Employee, NotUsed] = Source(
    Employee(Worker("Michallis"), System.currentTimeMillis, "@Architect and @CTO and @CEO and @TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Guillaume"), System.currentTimeMillis, "@MediorDeveloper for Trust1Team") ::
      Employee(Worker("Gilles"), System.currentTimeMillis, "@JuniorDeveloper for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "@SeniorDeveloper for Trust1Team") ::
      Employee(Worker("Michallis"), System.currentTimeMillis, "@Architect and @CTO and @CEO and @TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "@SeniorDeveloper for Trust1Team") ::
      Nil)

  implicit val system = ActorSystem("super-employees")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  employees
    .map(_.getJobs) // haal alle descriptions op
    .reduce(_ ++ _) // breng ze naar 1 set, duplicaten als ze er in zitten halen we er uit
    /**
      * Transform each input element into an `Iterable` of output elements that is
      * then flattened into the output stream.
      *
      * A stream is given as a stream of sequence of elements, but a stream of elements needed instead, streaming all the nested elements inside the sequences separately.
      * he mapConcat operation can be used to implement a one-to-many transformation of elements using a mapper function in the form of In => immutable.Seq[Out] .
      * In this case we want to map a Seq of elements to the elements in the collection itself, so we can call mapConcat(identity) .
      * voorbeeld:
      * val myData: Source[List[Message], NotUsed] = someDataSource
      * val flattened: Source[Message, NotUsed] = myData.mapConcat(identity)
      *
      * in ons geval hebben we een stream van tweets maar willen we een stream van hashtags dus gebruiken we de mapconcat om die stream te flattenen naar een hashtag stream
      */
    .mapConcat(identity)
    .map(jobs => JobDescription(jobs.name, jobs.proffesion.toUpperCase())) // Convert all hashtags to upper case, simpele map over elk element in de flow
    .runWith(Sink.foreach(job => println("geia sou " + job.name + " " + job.proffesion))) // Attach the Flow to a Sink that will finally print the hashtags
    .onComplete(_ => {
    println("done and terminating")
    system.terminate()
  })

}


object Reuseability extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val factorials = source.scan(BigInt(1))((acc, next) ⇒ acc * next)

  // runwith returned een materialized view van die flow (source naar sink)
  //FLOW1
  factorials.map(_.toString).runWith(lineSink("factorial2.txt"))

  // FLOW2
  factorials
    .zipWith(Source(0 to 100))((num, idx) ⇒ s"$idx! = $num")
    // we gaan de stream throttelen naar 1 element per seconde
    /** de throttle operator gaat zeggen dat hij maar x aantal element per seconde kan verwerken daardoor gaat die de upstream sources een melding geven dat hij maar zoveel aan kan
      * en gaat hij automatisch backpressure toevoegen aan de upstream
      */
    .throttle(1, FiniteDuration(1, TimeUnit.SECONDS))
    .runForeach(println)

  // FLOW3
  val result: Future[IOResult] =
    factorials
      .map(num ⇒ ByteString(s"$num\n"))
      .runWith(FileIO.toPath(Paths.get("factorials.txt")))

  // default dispatcher/execution context
  implicit val ec = system.dispatcher
  result.onComplete(_ ⇒ {
    println("done and terminating")
    system.terminate()
  })

  // de lineSink gaat een sink aanvaarden van strings en gaat een future[IOResult maken als een auxillary]
  def lineSink(filename: String): Sink[String, Future[IOResult]] =
  // input van de stream is een string value en we returnen de flow
  //Flow[In,Out,M2]
    Flow[String]
      // die string value omzetten naar een bytestring
      .map(s ⇒ ByteString(s + "\n"))
      // keep right zorgt uervoor dat we de rechtse materialized value behoden dus in ons geval de Future[IOResult], waar de aangepaste file dus in zit
      // hier maken we een nieuwe Sink[In, M3]
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
}


object ReactiveExample extends App {

  final case class Worker(name: String)

  final case class JobDescription(name: String, proffesion: String)

  final case class Employee(worker: Worker, startedOn: Long, description: String) {

    def getJobs: Set[JobDescription] = description.split(" ").collect {
      case t if t.startsWith("@") ⇒ JobDescription(worker.name, t)
    }.toSet
  }

  //Source[Out,M1]
  val employees: Source[Employee, NotUsed] = Source(
    Employee(Worker("Michallis"), System.currentTimeMillis, "@Architect and @CTO and @CEO and @TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Guillaume"), System.currentTimeMillis, "@Developer for Trust1Team") ::
      Employee(Worker("Gilles"), System.currentTimeMillis, "@Developer for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "@Developer for Trust1Team") ::
      Employee(Worker("Michallis"), System.currentTimeMillis, "@Architect and @CTO and @CEO and @TheOneWithTheMagic for Trust1Team") ::
      Employee(Worker("Jonas"), System.currentTimeMillis, "@Developer for Trust1Team") ::
      Nil)

  implicit val system = ActorSystem("super-employees")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  employees
    .map(_.getJobs)
    .reduce(_ ++ _)
    .mapConcat(identity)
    .map(jobs => JobDescription(jobs.name, jobs.proffesion.toUpperCase()))
    .runWith(Sink.foreach(job => println("geia sou " + job.name + " " + job.proffesion))) // hier is de Sink[In, M3]
    .onComplete(_ => {
    println("done and terminating")
    system.terminate()
  })
}