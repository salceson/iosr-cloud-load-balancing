import sbt._
import Keys._

object CommonSettings {
  private val ScrimageVersion = "2.1.7"
  private val AkkaVersion = "2.4.14"

  val commonDependencies: Seq[ModuleID] = Seq(
    "com.sksamuel.scrimage" %% "scrimage-core" % ScrimageVersion,
    "com.sksamuel.scrimage" %% "scrimage-io-extra" % ScrimageVersion,
    "com.sksamuel.scrimage" %% "scrimage-filters" % ScrimageVersion,
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    "com.typesafe.akka" %% "akka-remote" % AkkaVersion,

    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test",
    "org.specs2" %% "specs2-core" % "3.0" % "test"
  )

  val commonResolvers: Seq[MavenRepository] = Seq(
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
  )

  val commonTestScalacSettings: Seq[String] = Seq("-Yrangepos")

  val commonScalacSettings: Seq[String] = Seq("-Xmax-classfile-name", "78")

  val commonSettings: Seq[Def.Setting[_]] = Seq(
    resolvers ++= commonResolvers,
    scalacOptions ++= commonScalacSettings,
    scalacOptions in Test ++= commonTestScalacSettings,
    libraryDependencies ++= commonDependencies
  )
}
