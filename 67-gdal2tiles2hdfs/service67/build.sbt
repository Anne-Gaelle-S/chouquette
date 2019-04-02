name := "service67"
version := "1.0"

scalaVersion := "2.12.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.1" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test
)
