scalaVersion in ThisBuild := "2.12.7"
version in ThisBuild := "0.1.0"
scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided"

lazy val root = (project in file("."))
  .aggregate(webGateway)

lazy val core = (project in file("core"))
  .settings(
    name := "marimo-core",
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.6.6" % "test",
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val cli = (project in file("cli"))
  .settings(
    name := "marimo-cli",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "fastparse" % "2.1.0",
      "com.lihaoyi" %% "utest" % "0.6.6" % "test"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val fieldApi = (project in file("field-api"))
  .settings(
    libraryDependencies += lagomScaladslApi
  )

lazy val webGateway = (project in file("web-gateway"))
  .enablePlugins(PlayScala && LagomPlay)
  .settings(
    libraryDependencies ++= Seq(
      macwire
    )
  )
