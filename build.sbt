name := "ddb_client_scala"

version := "0.1"

scalaVersion := "2.11.8"

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
  "-Ypatmat-exhaust-depth", "off",
  "-Xfuture"
)

scalacOptions ++= compilerOptions

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-core" % "6.35.0",
  "org.scodec"  %% "scodec-core"  % "1.10.0",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"
)
