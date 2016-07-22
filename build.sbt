name := "Units-Toolbox"

lazy val commonSettings = Seq(
  organization := "com.hnrklssn.units-toolbox",
  version := "0.1.1",
  scalaVersion := "2.11.8"
)

val akkaV       = "2.4.8"
val scalaTestV  = "2.2.6"

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.12.0"
libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3"
libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3"
libraryDependencies += "org.scala-lang" %% "scala-pickling" % "0.8.0"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % akkaV
libraryDependencies += "org.scalatest"     %% "scalatest" % scalaTestV % "test"
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.8"

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "units-toolbox"
  )
