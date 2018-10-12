name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.16"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "co.fs2" %% "fs2-reactive-streams" % "1.0.0",
  "org.scodec" %% "scodec-stream" % "1.2.0",
  "org.scodec" %% "scodec-bits" % "1.1.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
