package example

object Main {
  def main(args: Array[String]): Unit = {
    val greeter = new Greeter(Person("Gilles", 25))
    greeter.greet()

    val f = new FinalMalaka
    println(f.msg)
    println(f.superMalaka)

    val salaries = List(1234.123,1294123.123,1239.123,12309123.111)
    SalaryRaiser.hugePromotion(salaries).map(res => {
      println(res)
    })

    println(UrlBuilderObject.url)

    println(NestedFunction.multiplyWithMalaka(84))

    CurryingExample.curryExample()
    CurryingExample.singleFunctionParam()


    PatternMatching.patternGuard()
    companion.companionExample()

    Extractor.ExtractorExample()

    ForComprehension.example()

    Generics.example()
    Generics.example2()

    AbstractType.newIntSeqBuf(9,35)
  }
}

// case class, kan max 22 props hebben je hoeft geen new voor te instantieren en props zijn immutable en hebben automatische comparing mogelijkheid
case class Person(name: String = "malaka", age: Int = 21)

class Greeter(person: Person) extends GreetingTrait {

  // Objecten kan je zien al singletons van hun eigen klasse
  // Def =  single instances of their own definitions
  object Greeting {
    // vaste waarde binnen de signleton
    var greeting = "hello"

    // Object definition can change the state of the singleton's values
    def changeGreeting(newGreeting: String): Unit = {
      greeting = newGreeting
    }
  }

  def greet (): Unit = {
    println(Greeting.greeting + " " + person.name)
  }

  override def toString: String =
    s"(${person.name}, ${person.age})"

}

// Trait kan je zien als een interface, definities kunnen default behavior hebben
trait GreetingTrait {
  def sayMalaka(): Unit = {
    println("malaka")
  }
  def saySomething(something: String): Unit = {
    println(something)
  }
  def addTo(first: Int, second: Int): Int = {
    first + second
  }
}



// example voor class composition with mixins
abstract class AbstractMalaka {
  val msg: String
}
class Malaka extends AbstractMalaka {
  val msg = "I'm Malaka"
}
trait TraitMalaka extends AbstractMalaka {
  def superMalaka = msg.toUpperCase()
}

// Malaka is een superclass, maar 1 superclass mogelijk
// TraitMalaka is een mixin, meerdere traits mogelijk
class FinalMalaka extends Malaka with TraitMalaka



//HIGHER-ORDER FUNCTIONS
// Higher order functions take other functions as parameters or return a function as a result
// is een functie die functies als parameters kunnen ontvangen en die dan een functie uitspuwen --> map

object SalaryRaiser {

  private def promotion(salaries: List[Double], promotionFunction: Double => Double): List[Double] =
    salaries.map(promotionFunction)

  def smallPromotion(salaries: List[Double]): List[Double] =
    promotion(salaries, salary => salary * 1.1)

  def bigPromotion(salaries: List[Double]): List[Double] =
    promotion(salaries, salary => salary * math.log(salary))

  def hugePromotion(salaries: List[Double]): List[Double] =
    promotion(salaries, salary => salary * salary)
}

// Function die functie returned
object UrlBuilderObject {
  def urlBuilder(ssl: Boolean, domain: String): (String, String) => String = {
    val schema = if (ssl) "https://" else "http://"
    (endpoint: String, query: String) => s"$schema$domain/$endpoint?$query"
  }
  val domainName = "www.trust1team.be"
  // returns a anon function
  def getURL: (String, String) => String = urlBuilder(ssl=true, domainName)
  val endpoint = "malakas"
  val query = "id=Gilles"
  // uses the anon function to create the url based on the params for the anon function
  val url = getURL(endpoint, query)
}


// Nested Functions
object NestedFunction {
  def multiplyWithMalaka(value: Int) = {
    def multiplier(x: Int, multiplier: Int): Int = {
      x * multiplier
    }
    s"${multiplier(value, 3)} Malaka"
  }
}


// TODO: Currying!!! (multiple parameter lists)
object CurryingExample {
  def curryExample() = {
    val numbers = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val res = numbers.foldLeft(0)((m, n) => m + n)
    println(res) // 55
  }

  def singleFunctionParam() = {
    val numbers = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    // fix the first parameter
    val numberFunc = numbers.foldLeft(List[Int]())_

    val squares = numberFunc((xs, x) => xs:+ x*x)
    println(squares.toString()) // List(1, 4, 9, 16, 25, 36, 49, 64, 81, 100)

    val cubes = numberFunc((xs, x) => xs:+ x*x*x)
    println(cubes.toString())  // List(1, 8, 27, 64, 125, 216, 343, 512, 729, 1000)
  }
}

object PatternMatching {
  abstract class Notification

  case class Email(sender: String, title: String, body: String) extends Notification

  case class SMS(caller: String, message: String) extends Notification

  case class VoiceRecording(contactName: String, link: String) extends Notification

  // matching on case classes and returning a custom string based on the class and its params
  def showNotification(notification: Notification): String = {
    notification match {
      case Email(email, title, _) =>
        s"You got an email from $email with title: $title"
      case SMS(number, message) =>
        s"You got an SMS from $number! Message: $message"
      case VoiceRecording(name, link) =>
        s"you received a Voice Recording from $name! Click the link to hear it: $link"
    }
  }

  // pattern guarding does the same but adds an if statement that can infer extra check
  def patternGuard() = {
    def showImportantNotification(notification: Notification, importantPeopleInfo: Seq[String]): String = {
      notification match {
        case Email(email, _, _) if importantPeopleInfo.contains(email) =>
          "You got an email from special someone!"
        case SMS(number, _) if importantPeopleInfo.contains(number) =>
          "You got an SMS from special someone!"
        case other =>
          showNotification(other) // nothing special, delegate to our original showNotification function
      }
    }

    val importantPeopleInfo = Seq("867-5309", "jenny@gmail.com")

    val someSms = SMS("867-5309", "Are you there?")
    val someVoiceRecording = VoiceRecording("Tom", "voicerecording.org/id/123")
    val importantEmail = Email("jenny@gmail.com", "Drinks tonight?", "I'm free after 5!")
    val importantSms = SMS("867-5309", "I'm here! Where are you?")

    println(showImportantNotification(someSms, importantPeopleInfo))
    println(showImportantNotification(someVoiceRecording, importantPeopleInfo))
    println(showImportantNotification(importantEmail, importantPeopleInfo))
    println(showImportantNotification(importantSms, importantPeopleInfo))
  }
}


object companion {
  def companionExample() = {
    val scalaCenterEmail = Email.fromString("scala.center@epfl.ch")
    scalaCenterEmail match {
      case Some(email) => println(
        s"""Registered an email
           |Username: ${email.username}
           |Domain name: ${email.domainName}
     """)
      case None => println("Error: could not parse email")
    }
  }
  class Email(val username: String, val domainName: String)

  // companion object for email, has a factory method inside it
  object Email {
    def fromString(emailString: String): Option[Email] = {
      emailString.split('@') match {
        case Array(a, b) => Some(new Email(a, b))
        case _ => None
      }
    }
  }
}

object Extractor {
  object CustomerID {
    import scala.util.Random

    // creates a string from the name
    def apply(name: String) = s"$name--${Random.nextLong}"

    // gets the name from a string
    def unapply(customerID: String): Option[String] = {
      val stringArray: Array[String] = customerID.split("--")
      if (stringArray.tail.nonEmpty) Some(stringArray.head) else None
    }
  }

  def ExtractorExample() = {
    val customer1ID = CustomerID("Sukyoung")
    customer1ID match {
      case CustomerID(name) => println(name)
      case _ => println("Could not extract a CustomerID")
    }
  }
}

object ForComprehension {
  case class User(name: String, age: Int)

  val userBase = List(User("Travis", 28),
    User("Kelly", 33),
    User("Jennifer", 44),
    User("Dennis", 23))

  def example() = {
    val twentySomethings = for (user <- userBase if (user.age >=20 && user.age < 30))
      yield user.name // this will add the user name to a new list

    twentySomethings.foreach(name => println(name))
  }

}

object Generics {
  // A is a convention for a generic
  // this is a simple generic List class
  class Stack[A] {
    private var elements: List[A] = Nil
    def push(x: A) { elements = x :: elements }
    def peek: A = elements.head
    def pop(): A = {
      val currentTop = peek
      elements = elements.tail
      currentTop
    }
  }

  def example() = {
    val stack = new Stack[Int]
    stack.push(1)
    stack.push(2)
    println(stack.pop)  // prints 2
    println(stack.pop)  // prints 1
  }
  def example2() = {
    class Fruit
    class Apple extends Fruit
    class Banana extends Fruit

    // here we can add different fruit subtypes because we created a list of the parent type
    val stack = new Stack[Fruit]
    val apple = new Apple
    val banana = new Banana

    stack.push(apple)
    stack.push(banana)
  }
}

object Variances {
  class Foo[+A] // A covariant class
  class Bar[-A] // A contravariant class
  class Baz[A]  // An invariant class

  // covariance
  // Cat and Dog are a subtype of Animal, both are Animals
  abstract class Animal {
    def name: String
  }
  case class Cat(name: String) extends Animal
  case class Dog(name: String) extends Animal


  // contravariance
  //contravariant implies that for two types A and B where A is a subtype of B, Writer[B] is a subtype of Writer[A].
  // so animal is a subtype of Dog and Cat
  abstract class Printer[-A] {
    def print(value: A): Unit
  }

  class AnimalPrinter extends Printer[Animal] {
    def print(animal: Animal): Unit =
      println("The animal's name is: " + animal.name)
  }

  class CatPrinter extends Printer[Cat] {
    def print(cat: Cat): Unit =
      println("The cat's name is: " + cat.name)
  }

  // invariance
 // A cat is not a animal and a animal is not a cat
}

object AbstractType {
  // Abstract types, such as traits and abstract classes, can in turn have abstract type members. This means that the concrete implementations define the actual types
  trait Buffer {
    type T
    val element: T
  }
  abstract class SeqBuffer extends Buffer {
    type U
    // upper bound type T
    type T <: Seq[U]
    def length = element.length
  }
  abstract class IntSeqBuffer extends SeqBuffer {
    type U = Int
  }


  def newIntSeqBuf(elem1: Int, elem2: Int): IntSeqBuffer =
    new IntSeqBuffer {
      type T = List[U]
      val element = List(elem1, elem2)
    }
  val buf = newIntSeqBuf(7, 8)
  println("length = " + buf.length)
  println("content = " + buf.element)

}

object CompoundTypes {
  trait Cloneable extends java.lang.Cloneable {
    override def clone(): Cloneable = {
      super.clone().asInstanceOf[Cloneable]
    }
  }
  trait Resetable {
    def reset: Unit
  }

  // this makes it possible to tell that object is a subtype of several types
  // this means we can use the clonable and resetable trait functionality on 1 object
  def cloneAndReset(obj: Cloneable with Resetable): Cloneable = {
    val cloned = obj.clone()
    obj.reset
    cloned
  }
}


object SelfType {
  trait User {
    def username: String
  }

  trait Tweeter {
    // here we reassign this
    this: User =>
    def tweet(tweetText: String) = println(s"$username: $tweetText")
  }

  // using tiwht because Tweeter requires it
  class VerifiedTweeter(val username_ : String) extends Tweeter with User {
    def username = s"real $username_"
  }

  def example() = {
    val realMalaka = new VerifiedTweeter("Malaka")
    realMalaka.tweet("wrote some swell code")
  }
}