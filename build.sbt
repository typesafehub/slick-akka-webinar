name := "slick-akka-webinar"
 
scalaVersion := "2.11.6"
 
val akkaVersion = "1.0-RC2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-scala-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-xml-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "com.zaxxer" % "HikariCP-java6" % "2.3.7",
  "com.h2database" % "h2" % "1.3.170",
  "org.slf4j" % "slf4j-nop" % "1.7.7"
)
