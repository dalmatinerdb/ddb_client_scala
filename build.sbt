name := "ddb_client_scala"

version := "0.1"

scalaVersion := "2.10.5"

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-language:implicitConversions",
  "-Yinline-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Xfuture"
)

scalacOptions ++= compilerOptions

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-core" % "6.35.0",
  "org.scodec"  %% "scodec-core"  % "1.10.0",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
  "org.xerial.snappy" % "snappy-java" % "1.1.3-M2",
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)
