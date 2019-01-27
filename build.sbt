scalaVersion in ThisBuild := "2.12.7"
version in ThisBuild := "0.1.0"

scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

lazy val core = (project in file("core"))
  .settings(
    name := "marimo-core",
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.6.6" % "test",
    testFrameworks += new TestFramework("utest.runner.Framework")
  )