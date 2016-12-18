name := "supervisor"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.docker-java" % "docker-java" % "3.0.6"
)

assemblyJarName in assembly := "supervisor.jar"

mainClass := Some("iosr.supervisor.SupervisorApp")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}