import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

scalaVersion in ThisBuild := "2.12.7"
version in ThisBuild := "0.1.0"
scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "4.0.0"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided"

lazy val root = (project in file("."))
  .aggregate(
    playerApi, playerImpl,
    fieldApi, fieldImpl,
    webGateway
  )

lazy val core = (project in file("core"))
  .settings(
    name := "marimo-core",
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.6.6" % "test",
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js

lazy val cli = (project in file("cli"))
  .settings(
    name := "marimo-cli",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "fastparse" % "2.1.0",
      "com.lihaoyi" %% "utest" % "0.6.6" % "test"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val js = (project in file("js"))
  .settings(
    name := "marimo-js",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "io.suzaku" %%% "boopickle" % "1.3.0"
    )
  )
  .dependsOn(sharedJS)
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)

lazy val playerApi = (project in file("player-api"))
  .settings(
    name := "player-api",
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )

lazy val playerImpl = (project in file("player-impl"))
  .settings(
    name := "player-impl",
    libraryDependencies ++= Seq(
      macwire
    )
  )
  .enablePlugins(LagomScala)
  .dependsOn(playerApi)

lazy val fieldApi = (project in file("field-api"))
  .settings(
    name := "field-api",
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )

lazy val fieldImpl = (project in file("field-impl"))
  .settings(
    name := "field-impl",
    libraryDependencies ++= Seq(
      macwire
    )
  )
  .enablePlugins(LagomScala)
  .dependsOn(fieldApi)


lazy val webGateway = (project in file("web-gateway"))
  .enablePlugins(PlayScala && LagomPlay)
  .dependsOn(playerApi, fieldApi, sharedJVM)
  .settings(
    name := "web-gateway",
    libraryDependencies ++= Seq(
      macwire,
      "com.vmunier" %% "scalajs-scripts" % "1.1.2",
      "io.suzaku" %% "boopickle" % "1.3.0"
    ),
    scalaJSProjects := Seq(js),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value
  )
