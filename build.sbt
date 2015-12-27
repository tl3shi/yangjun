import play.Play.autoImport._
import play.PlayScala
import sbt.Keys._
import sbt._

name := "zhtool"

version := "1.0"

scalaVersion := "2.10.4"

resolvers += "local maven" at ("file://%s/.m2/repository" format Path.userHome.absolutePath)

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.7",
  "com.typesafe.akka" %% "akka-actor" % "2.3.12",
  "com.typesafe" %% "scalalogging-slf4j" % "1.1.0",
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1",
  "com.typesafe.play" %% "play-slick" % "0.6.0.1",
  "org.apache.commons" % "commons-lang3" % "3.1"
)

unmanagedBase := baseDirectory.value / "lib"


lazy val root = (project in file(".")).enablePlugins(PlayScala)
