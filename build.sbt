name := "Replicated-log"
version := "0.1"
scalaVersion := "2.13.6"

val AkkaVersion     = "2.6.16"
val AkkaHttpVersion = "10.2.6"
val ScalaLogging    = "3.9.4"
val logback         = "1.2.6"
val janino          = "3.1.2"
val akkaPlayJson    = "1.38.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka"          %% "akka-actor-typed"    % AkkaVersion,
  "com.typesafe.akka"          %% "akka-stream"         % AkkaVersion,
  "com.typesafe.akka"          %% "akka-stream"         % AkkaVersion,
  "com.typesafe.akka"          %% "akka-http"           % AkkaHttpVersion,
  "com.typesafe.scala-logging" %% "scala-logging"       % ScalaLogging,
  "com.typesafe.akka"          %% "akka-slf4j"          % AkkaVersion,
  "ch.qos.logback"             % "logback-classic"      % logback,
  "org.codehaus.janino"        % "janino"               % janino,
  "de.heikoseeberger"          %% "akka-http-play-json" % akkaPlayJson
)
