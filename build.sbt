name := "ddb_client_scala"

organization := "dalmatinerdb"

version := "0.1"

scalaVersion := "2.11.7"

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
  "org.xerial.snappy" % "snappy-java" % "1.1.3-M2"
)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

lazy val assemblySettings = Seq(
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
  test in assembly := {},
  artifact in (Compile, assembly) := {
    val art = (artifact in (Compile, assembly)).value
    art.copy(`classifier` = Some("assembly"))
  }
) ++ addArtifact(artifact in (Compile, assembly), assembly).settings

lazy val root = (project in file(".")).settings(assemblySettings)
