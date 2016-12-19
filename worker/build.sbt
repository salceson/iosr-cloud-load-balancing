import sbtassembly.AssemblyKeys.assemblyOutputPath
import sbtdocker.DockerKeys.dockerfile

name := "worker"

version := "1.0"

scalaVersion := "2.11.8"

assemblyJarName in assembly := "worker.jar"

mainClass := Some("iosr.worker.WorkerApp")

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = artifact.name
  new sbtdocker.mutable.Dockerfile {
    from("frolvlad/alpine-oraclejdk8:slim")
    copy(artifact, artifactTargetPath)
    env("WORKERADDRESS" -> "")
    env("SUPERVISORADDRESS" -> "supervisor:6000")
    env("MONITORINGADDRESS" -> "monitoring:5000")
    entryPoint("sh", "-c", s"java -Dakka.remote.netty.tcp.hostname=$$WORKERADDRESS -Dsupervisor.address=$$SUPERVISORADDRESS -Dmonitoring.address=$$MONITORINGADDRESS -jar ${artifact.name}"
    )
  }
}
