import CommonSettings._

name := "filters"

version := "1.0"

scalaVersion := "2.11.8"

lazy val common = (project in file("common")).settings(commonSettings)

lazy val scaleFilter = (project in file("scale-filter")).settings(commonSettings).dependsOn(common)

lazy val filters = (project in file(".")).aggregate(common, scaleFilter)
