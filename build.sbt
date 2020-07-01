
scalaVersion := "2.13.2"

lazy val root = (project in file("."))
  .settings(
    name := "akka_tcp_example",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.6"
  )


