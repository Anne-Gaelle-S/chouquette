name := "chouquette"
version := "1.0"

scalaVersion := "2.12.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// fork in Test := false

coverageEnabled := true
coverageMinimum := 95
// coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;router.Routes.*;chouquette.controllers.javascript.*"

libraryDependencies ++= Seq(
  guice,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.1" % Test,
  "org.scalamock"          %% "scalamock"          % "4.1.0" % Test
)
