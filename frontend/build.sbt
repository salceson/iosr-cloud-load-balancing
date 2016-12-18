import sbtassembly.AssemblyKeys.assemblyOutputPath
import sbtdocker.DockerKeys.dockerfile

name := "frontend"

version := "1.0"

scalaVersion := "2.11.8"

val AkkaHttpVersion = "10.0.0"
val AkkaHttpJson4sVersion = "1.11.0"
val Json4sVersion = "3.5.0"

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.bintrayRepo("hseeberger", "maven")
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "org.json4s" %% "json4s-native" % Json4sVersion,
  "de.heikoseeberger" %% "akka-http-json4s" % AkkaHttpJson4sVersion,

  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % "test"
)

assemblyJarName in assembly := "frontend.jar"
mainClass := Some("iosr.frontend.FrontendApp")

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = artifact.name
  new sbtdocker.mutable.Dockerfile {
    from("frolvlad/alpine-oraclejdk8:slim")
    copy(artifact, artifactTargetPath)
    env("SUPERVISORADDRESS" -> "")
    entryPoint("sh", "-c", "java", s"-Dsupervisor.address=$$SUPERVISORADDRESS -jar ${artifact.name}")
  }
}
