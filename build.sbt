Global / onChangedBuildSource := ReloadOnSourceChanges

//
//ThisBuild / organization := "com.xebia"
//ThisBuild / scalaVersion := "3.3.0"
//
//addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; ++test")
//addCommandAlias("ci-docs", "github; headerCreateAll")
//addCommandAlias("ci-publish", "github; ci-release")
//
//lazy val root = (project in file("."))
//  .settings(
//    name := "karat-scalacheck",
//    version := "0.0.1-SNAPSHOT",
//    libraryDependencies ++= Seq(
//      "org.typelevel"  %% "cats-core"               % "2.9.0",
//      "org.scalacheck" %% "scalacheck"              % "1.17.0",
//      "com.47deg.karat" % "karat-common-jvm"        % "0.1.2",
//      "org.scalameta"  %% "munit"                   % "0.7.29"  % Test,
//      "org.typelevel"  %% "cats-effect"             % "3.5.0"   % Test,
//      "org.typelevel"  %% "scalacheck-effect"       % "1.0.4"   % Test,
//      "org.typelevel"  %% "munit-cats-effect-3"     % "1.0.7"   % Test,
//      "org.typelevel"  %% "scalacheck-effect-munit" % "1.0.4"   % Test,
//      "org.http4s"     %% "http4s-ember-server"     % "0.23.20" % Test,
//      "org.http4s"     %% "http4s-ember-client"     % "0.23.20" % Test,
//      "org.http4s"     %% "http4s-circe"            % "0.23.20" % Test,
//      "org.http4s"     %% "http4s-dsl"              % "0.23.20" % Test,
//      "io.circe"       %% "circe-generic"           % "0.14.5"  % Test
//    ),
//    testFrameworks += new TestFramework("munit.Framework")
//  )

import sbt.internal.ProjectMatrix

val scala2_12 = "2.12.18"
val scala2_13 = "2.13.11"
val scala3 = "3.3.0"
val allScalaVersions = List(scala2_12, scala2_13, scala3)

ThisBuild / organization := "com.xebia"
ThisBuild / scalaVersion := scala2_13
publish / skip := true

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/xebia-functional/karat-scalacheck"),
    "scm:git:https://github.com/xebia-functional/karat-scalacheck.git"
  )
)

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; mdoc; ++test")
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll")
addCommandAlias("ci-publish", "github; ci-release")

lazy val documentation = project
  .settings(
    publish / skip := true,
    mdocOut := file(".")
  )
  .enablePlugins(MdocPlugin)

lazy val `karat-scalacheck`: ProjectMatrix =
  (projectMatrix in file("modules/karat-scalacheck"))
    .settings(description := "TODO")
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel"          %% "cats-core"               % "2.9.0",
        "org.scalacheck"         %% "scalacheck"              % "1.17.0",
        "com.47deg.karat"         % "karat-common-jvm"        % "0.1.2",
        "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0",
        "org.scalameta"          %% "munit"                   % "0.7.29" % Test,
        "org.typelevel"          %% "cats-effect"             % "3.5.0"  % Test,
        "org.typelevel"          %% "munit-cats-effect-3"     % "1.0.7"  % Test,
        "org.typelevel"          %% "scalacheck-effect"       % "1.0.4"  % Test,
        "org.typelevel"          %% "scalacheck-effect-munit" % "1.0.4"  % Test
      ),
      scalacOptions --= Seq("-Werror", "-Xfatal-warnings")
    )
    .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `karat-scalacheck-effect`: ProjectMatrix =
  (projectMatrix in file("modules/karat-scalacheck-effect"))
    .dependsOn(`karat-scalacheck`)
    .settings(description := "TODO")
    .settings(libraryDependencies += "org.typelevel" %% "scalacheck-effect" % "1.0.4")
    .settings(scalacOptions --= Seq("-Werror", "-Xfatal-warnings"))
    .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `karat-scalacheck-http4s-sample`: ProjectMatrix =
  (projectMatrix in file("modules/karat-scalacheck-http4s-sample"))
    .dependsOn(`karat-scalacheck-effect` % Test)
    .settings(description := "TODO")
    .settings(publish / skip := true)
    .settings(
      libraryDependencies ++= Seq(
        "org.http4s"    %% "http4s-ember-server"     % "0.23.20",
        "org.http4s"    %% "http4s-ember-client"     % "0.23.20",
        "org.http4s"    %% "http4s-circe"            % "0.23.20",
        "org.http4s"    %% "http4s-dsl"              % "0.23.20",
        "io.circe"      %% "circe-generic"           % "0.14.5",
        "org.typelevel" %% "munit-cats-effect-3"     % "1.0.7" % Test,
        "org.typelevel" %% "scalacheck-effect"       % "1.0.4" % Test,
        "org.typelevel" %% "scalacheck-effect-munit" % "1.0.4" % Test
      )
    )
    .settings(scalacOptions --= Seq("-Werror", "-Xfatal-warnings"))
    .jvmPlatform(scalaVersions = allScalaVersions)
