import sbt._

lazy val zioqueue = (project in file(".")).
  settings (
    name := "zioqueue",
    organization := "org.justinhj",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.12.6"
    // add other settings here
  )

/* scala versions and options */
scalaVersion := "2.12.6"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")

// These options will be used for *all* versions.
scalacOptions ++= Seq(
  "-deprecation"
  , "-unchecked"
  , "-encoding", "UTF-8"
  , "-Xlint"
  , "-Xverify"
  , "-feature"
  ,"-Ypartial-unification"
  //,"-Xfatal-warnings"
  , "-language:_"
  //,"-optimise"
)

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-source", "1.7", "-target", "1.7")

// javaOptions in Universal ++= Seq(
//   "-J-server",
//   "-J-Xms1g -Xmx4g",
//   "-J-XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled",
//   "-J-XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=68",
//   "-J-XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark",
//   "-J-XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M"
// )

val ScalaZVersion = "7.3.0-M28"
val ZIOVersion = "1.0-RC1"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  // -- testing --
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  // -- Logging --
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  // -- json parsing
  "com.lihaoyi" %% "upickle" % "0.7.1",
  // scalaz
  "org.scalaz" %% "scalaz-core" % ScalaZVersion,
  "org.scalaz" %% "scalaz-zio" % ZIOVersion,
  // type classes
  "com.github.mpilquist" %% "simulacrum" % "0.12.0",
  // Application
  "org.ocpsoft.prettytime" % "prettytime" % "3.2.7.Final",
  "org.scalaj" %% "scalaj-http" % "2.4.1",
  // li haoyi ammonite repl embed
  "com.lihaoyi" % "ammonite" % "1.1.2" % "test" cross CrossVersion.full
)

//ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

resolvers ++= Seq(
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Secured Central Repository" at "https://repo1.maven.org/maven2",
  Resolver.sonatypeRepo("snapshots")
)

// scalariform
//scalariformSettings

// ScalariformKeys.preferences := ScalariformKeys.preferences.value
//   .setPreference(AlignSingleLineCaseStatements, true)
//   .setPreference(DoubleIndentClassDeclaration, true)
//   .setPreference(IndentLocalDefs, true)
//   .setPreference(IndentPackageBlocks, true)
//   .setPreference(IndentSpaces, 2)
//   .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)

// ammonite repl
sourceGenerators in Test += Def.task {
  val file = (sourceManaged in Test).value / "amm.scala"
  IO.write(file, """object amm extends App { ammonite.Main().run() }""")
  Seq(file)
}.taskValue

