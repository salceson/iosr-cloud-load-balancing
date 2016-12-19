import sbtassembly.AssemblyKeys.assemblyOutputPath
import sbtdocker.DockerKeys.dockerfile

name := "monitoring"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.docker-java" % "docker-java" % "3.0.6",
  "com.google.code.findbugs" % "jsr305" % "2.0.3" % "provided"
)

assemblyJarName in assembly := "monitoring.jar"

mainClass := Some("iosr.monitoring.MonitoringApp")

assemblyMergeStrategy in assembly := {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList(ps @ _*) if Assembly.isSystemJunkFile(ps.last) =>
    MergeStrategy.discard
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case (x :: Nil) if Seq("manifest.mf", "index.list", "dependencies") contains x =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") || ps.last.endsWith(".rsa") =>
        MergeStrategy.discard
      case "maven" :: xs =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) | ("spring.tooling" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.first
    }
  case _ => MergeStrategy.first
}

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = artifact.name
  new sbtdocker.mutable.Dockerfile {
    from("frolvlad/alpine-oraclejdk8:slim")
    copy(artifact, artifactTargetPath)
    entryPoint("sh", "-c", s"java -Dakka.remote.netty.tcp.hostname=monitoring -jar ${artifact.name}")
  }
}
