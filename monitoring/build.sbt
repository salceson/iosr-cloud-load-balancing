name := "supervisor"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.docker-java" % "docker-java" % "3.0.6",
  "com.google.code.findbugs" % "jsr305" % "2.0.3" % "provided"
)

assemblyJarName in assembly := "monitoring.jar"

mainClass := Some("iosr.monitoring.MonitoringApp")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
