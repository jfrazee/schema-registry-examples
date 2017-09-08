import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

name := "schema-registry-examples"

organization := "io.atomicfinch"

scalaVersion := "2.11.11"

resolvers ++= Seq(
  DefaultMavenRepository,
  Resolver.bintrayRepo("typesafe", "releases"),
  Resolver.sonatypeRepo("releases"),
  "Apache Releases" at "https://repository.apache.org/content/repositories/releases/",
  "Apache Snapshots" at "https://repository.apache.org/content/repositories/snapshots/",
  Resolver.mavenLocal
)

val AvroVersion = "1.8.2"
val HortonworksRegistryVersion = "0.2.1"
val FlinkVersion = "1.3.2"
val TypesafeConfigVersion = "1.3.1"
val ScalatestVersion = "3.0.1"
val ScalacheckVersion = "1.13.4"
val JodaConvertVersion = "1.8"

val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % FlinkVersion % "provided",
  "org.apache.flink" %% "flink-streaming-scala" % FlinkVersion % "provided",
  "org.apache.flink" %% "flink-connector-kafka-0.10" % FlinkVersion % "provided"
)

libraryDependencies ++= Seq(
  "org.apache.avro" % "avro" % AvroVersion,
  "com.hortonworks.registries" % "schema-registry-serdes" % HortonworksRegistryVersion,
  "com.typesafe" %  "config" % TypesafeConfigVersion,
  "org.joda" % "joda-convert" % JodaConvertVersion,
  "org.scalatest" %% "scalatest" % ScalatestVersion % "test",
  "org.scalacheck" %% "scalacheck" % ScalacheckVersion % "test"
)

lazy val root = (project in file(".")).
  settings(libraryDependencies ++= flinkDependencies)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused-import",
  "-Xfuture"
)

mainClass in assembly :=
  Some("io.atomicfinch.examples.flink.SchemaRegistryExample")

run in Compile := Defaults.runTask(
    fullClasspath in Compile,
    mainClass in (Compile, run),
    runner in (Compile, run)
  ).evaluated

assemblyOption in assembly :=
  (assemblyOption in assembly).value.copy(includeScala = false)

fork in run := true

cancelable in Global := true

initialCommands := "import io.atomicfinch._"
