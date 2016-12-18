import sbtassembly.AssemblyKeys.assemblyOutputPath
import sbtdocker.DockerKeys.dockerfile

name := "supervisor"

version := "1.0"

scalaVersion := "2.11.8"

assemblyJarName in assembly := "supervisor.jar"

mainClass := Some("iosr.supervisor.SupervisorApp")

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = artifact.name
  new sbtdocker.mutable.Dockerfile {
    from("frolvlad/alpine-oraclejdk8:slim")
    copy(artifact, artifactTargetPath)
    entryPoint("sh", "-c", s"java -Dakka.remote.netty.tcp.hostname=supervisor -jar ${artifact.name}")
  }
}
