import sbtassembly.AssemblyKeys.assemblyOutputPath
import sbtdocker.DockerKeys.dockerfile

name := "tester"

version := "1.0"

scalaVersion := "2.11.8"

val AkkaHttpVersion = "10.0.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)

assemblyJarName in assembly := "tester.jar"

mainClass := Some("iosr.tester.TesterApp")

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = artifact.name
  new sbtdocker.mutable.Dockerfile {
    from("frolvlad/alpine-oraclejdk8:slim")
    copy(artifact, artifactTargetPath)
    env("FRONTENDADDRESS" -> "frontend:3000")
    entryPoint("sh", "-c",
      s"java -Dfrontend.address=$$FRONTENDADDRESS -Dakka.remote.netty.tcp.hostname=tester -jar ${artifact.name}")
  }
}
