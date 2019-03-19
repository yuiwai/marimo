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
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"

lazy val root = (project in file("."))
  .aggregate(
    worldApi, worldImpl,
    playerApi, playerImpl,
    fieldApi, fieldImpl,
    marketApi, marketImpl,
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

lazy val cli = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("cli"))
  .settings(
    name := "marimo-cli"
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "fastparse" % "2.1.0",
      "com.lihaoyi" %% "utest" % "0.6.6" % "test"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "fastparse" % "2.1.0",
    )
  )
lazy val cliJVM = cli.jvm
lazy val cliJS = cli.js

lazy val js = (project in file("js"))
  .settings(
    name := "marimo-js",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "io.suzaku" %%% "boopickle" % "1.3.0",
      "com.github.japgolly.scalajs-react" %%% "core" % "1.3.1"
    ),
    npmDependencies in Compile ++= Seq(
      "react" -> "16.5.1",
      "react-dom" -> "16.5.1"
    )
  )
  .dependsOn(sharedJS, cliJS)
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb, ScalaJSBundlerPlugin)

lazy val worldApi = (project in file("world-api"))
  .settings(
    name := "world-api",
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )

lazy val worldImpl = (project in file("world-impl"))
  .settings(lagomForkedTestSettings: _*)
  .settings(
    name := "world-impl",
    libraryDependencies ++= Seq(
      macwire,
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      scalaTest
    )
  )
  .enablePlugins(LagomScala)
  .dependsOn(worldApi)

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

lazy val marketApi = (project in file("market-api"))
  .settings(
    name := "market-api",
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )

lazy val marketImpl = (project in file("market-impl"))
  .settings(
    name := "market-impl",
    libraryDependencies ++= Seq(
      macwire,
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      scalaTest
    )
  )
  .enablePlugins(LagomScala)
  .dependsOn(marketApi)

lazy val webGateway = (project in file("web-gateway"))
  .enablePlugins(PlayScala, LagomPlay, WebScalaJSBundlerPlugin, SbtReactiveAppPlugin)
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
