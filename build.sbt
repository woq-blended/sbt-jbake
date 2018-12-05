sbtPlugin := true

name := "sbt-jbake"

organization := "de.wayofquality"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

version := "0.1.0-SNAPSHOT"

// We only support sbt-1.x
// crossSbtVersions := Seq("0.13.17", "1.2.7")

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-encoding", "UTF-8"
)
