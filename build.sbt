import CommonSettings._

organization := "iosr"

name := "iosr-cloud-load-balancing"

version := "1.0"

scalaVersion := "2.11.8"

lazy val common = (project in file("common"))
  .settings(commonSettings)

lazy val worker = (project in file("worker"))
  .settings(commonSettings)
  .settings(commonDockerSettings)
  .enablePlugins(DockerPlugin)
  .dependsOn(common)

lazy val frontend = (project in file("frontend"))
  .settings(commonSettings)
  .settings(commonDockerSettings)
  .enablePlugins(DockerPlugin)
  .dependsOn(common)

lazy val supervisor = (project in file("supervisor"))
  .settings(commonSettings)
  .settings(commonDockerSettings)
  .enablePlugins(DockerPlugin)
  .dependsOn(common)

lazy val monitoring = (project in file("monitoring"))
  .settings(commonSettings)
  .settings(commonDockerSettings)
  .enablePlugins(DockerPlugin)
  .dependsOn(common)

lazy val tester = (project in file("tester"))
  .settings(commonSettings)
  .settings(commonDockerSettings)
  .enablePlugins(DockerPlugin)

lazy val IOSRCloudLoadBalancing = (project in file("."))
  .aggregate(common, worker, frontend, supervisor, monitoring, tester)
