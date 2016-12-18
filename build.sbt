import CommonSettings._

name := "iosr-cloud-load-balancing"

version := "1.0"

scalaVersion := "2.11.8"

lazy val common = (project in file("common"))
  .settings(commonSettings)

lazy val worker = (project in file("worker"))
  .settings(commonSettings)
  .dependsOn(common)

lazy val frontend = (project in file("frontend"))
  .settings(commonSettings)
  .dependsOn(common)

lazy val IOSRCloudLoadBalancing = (project in file(".")).aggregate(common, worker, frontend)
