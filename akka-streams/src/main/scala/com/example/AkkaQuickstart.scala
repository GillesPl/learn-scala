//#full-example
package com.example

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.example.FirstSteps.{done, system}

import scala.concurrent._


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

object firstStepsTweet extends App {

  // author of a tweet
  final case class Author(handle: String)

  // acutal tweet hashtag
  final case class Hashtag(name: String)

  // tweet class
  final case class Tweet(author: Author, timestamp: Long, body: String) {
    // function that collects all the hashtags in the body and returns it as a set of Hashtags
    def hashtags: Set[Hashtag] = body.split(" ").collect {
      // returnes the tag without the hash (#test => test)
      case t if t.startsWith("#") ⇒ Hashtag(t.replaceAll("[^#\\w]", ""))
    }.toSet
  }

  // hier gaan we een source van tweets maken, dit is de blueprint van onze stream
  // later gaan we de stream uitvoeren maar die nog voor en tijdens de flow bewerken
  val tweets: Source[Tweet, NotUsed] = Source(
    Tweet(Author("Michallis"), System.currentTimeMillis, "#T1T rocks!") ::
      Tweet(Author("Guillaume"), System.currentTimeMillis, "#T1T!") ::
      Tweet(Author("Gilles"), System.currentTimeMillis, "#T1T !") ::
      Tweet(Author("Jonas"), System.currentTimeMillis, "#T1T !") ::
      Tweet(Author("Guillaume"), System.currentTimeMillis, "#T1T on the rocks!") ::
      Tweet(Author("Guillaume"), System.currentTimeMillis, "wow #T1T !") ::
      Tweet(Author("Guillaume"), System.currentTimeMillis, "#T1T rocks!") ::
      Tweet(Author("Michallis"), System.currentTimeMillis, "#malaka rock!") ::
      Tweet(Author("Michallis"), System.currentTimeMillis, "#Gamiseta rock!") ::
      Tweet(Author("poopooo"), System.currentTimeMillis, "we compared #Tjoepkes to #Tjapkes!") ::
      Nil)

  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()

  tweets
    .map(_.hashtags) // Get all sets of hashtags ...
    .reduce(_ ++ _) // ... and reduce them to a single set, removing duplicates across all tweets
    /**
      * Transform each input element into an `Iterable` of output elements that is
      * then flattened into the output stream.
      *
      * A stream is given as a stream of sequence of elements, but a stream of elements needed instead, streaming all the nested elements inside the sequences separately.
      * he mapConcat operation can be used to implement a one-to-many transformation of elements using a mapper function in the form of In => immutable.Seq[Out] . In this case we want to map a Seq of elements to the elements in the collection itself, so we can call mapConcat(identity) .
      * voorbeeld:
      * val myData: Source[List[Message], NotUsed] = someDataSource
      * val flattened: Source[Message, NotUsed] = myData.mapConcat(identity)
      *
      * in ons geval hebben we een stream van tweets maar willen we een stream van hashtags dus gebruiken we de mapconcat om die stream te flattenen naar een hashtag stream
    */
    .mapConcat(identity)
    .map(_.name.toUpperCase) // Convert all hashtags to upper case, simpele map over elk element in de flow
    .runWith(Sink.foreach(println)) // Attach the Flow to a Sink that will finally print the hashtags

}