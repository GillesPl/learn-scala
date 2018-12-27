name := "AkkaTest"
 
version := "1.0" 
      
lazy val `akkatest` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice,
  "io.swagger" %% "swagger-play2" % "1.6.0",
  "org.webjars" % "swagger-ui" % "3.9.2",
  "com.typesafe.play" %% "play-json" % "2.6.7"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

      