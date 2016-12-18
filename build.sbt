import CommonSettings._

name := "iosr-cloud-load-balancing"

version := "1.0"

scalaVersion := "2.11.8"

lazy val common = (project in file("common"))
  .settings(commonSettings)

lazy val worker = (project in file("worker"))
  .settings(commonSettings)
  .dependsOn(common)

lazy val gui = (project in file("gui"))
  .settings(commonSettings)
  .dependsOn(common)

lazy val supervisor = (project in file("supervisor"))
  .settings(commonSettings)
  .dependsOn(common)

lazy val monitoring = (project in file("monitoring"))
  .settings(commonSettings)
  .dependsOn(common)

lazy val IOSRCloudLoadBalancing = (project in file(".")).aggregate(common, worker, gui, supervisor, monitoring)
