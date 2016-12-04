import CommonSettings._

name := "iosr-cloud-load-balancing"

version := "1.0"

scalaVersion := "2.11.8"

lazy val filters = (project in file("filters")).settings(commonSettings)

lazy val gui = (project in file("gui")).settings(commonSettings)

lazy val IOSRCloudLoadBalancing = (project in file(".")).aggregate(filters, gui)
