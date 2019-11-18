
name := "akka-streams-kafka-last-n-minutes"

version := "0.1"

scalaVersion := "2.13.1"

scalacOptions := Seq(
  "-encoding", "utf8",
  "-target:jvm-1.8",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked",
  "-deprecation",
  "-Xlog-reflective-calls"
)

resolvers ++= Seq(
  Resolver.mavenCentral
)

val akkaVersion = "2.5.23"
val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"           %% "akka-actor"                  % akkaVersion,
  "com.typesafe.akka"           %% "akka-stream"                 % akkaVersion,
  "com.typesafe.akka"           %% "akka-stream-kafka"           % "1.0.4",

  "io.circe"                    %% "circe-core"                  % circeVersion,
  "io.circe"                    %% "circe-generic"               % circeVersion,
  "io.circe"                    %% "circe-parser"                % circeVersion,

  "com.typesafe.scala-logging"  %% "scala-logging"               % "3.9.2",

  "com.typesafe.akka"           %% "akka-stream-testkit"         % akkaVersion       % Test,
  "org.scalatest"               %% "scalatest"                   % "3.0.8"           % Test,
)
