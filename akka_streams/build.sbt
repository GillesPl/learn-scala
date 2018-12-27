name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.19",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.5.19",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.19" % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.5.19" % Test,
  "com.typesafe.akka" %% "akka-stream-typed" % "2.5.19",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
