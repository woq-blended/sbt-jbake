
publishMavenStyle := true

publishArtifact in Test := false

licenses += ("Apache 2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/woq-blended/sbt-jbake"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/woq-blended/sbt-jbake"),
    "scm:git@github.com:woq-blended/sbt-jbake.git"
  )
)

developers := List(
  Developer(id = "atooni", name = "Andreas Gies", email = "andreas@wayofquality.de", url = url("https://github.com/atooni")),
  Developer(id = "lefou", name = "Tobias Roeser", email = "tobias.roser@tototec.de", url = url("https://github.com/lefou"))
)


sonatypeProfileName := "de.wayofquality"

credentials ++= (for {
  username <- Option(System.getenv().get("SONATYPE_USERNAME"))
  password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
} yield Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  username,
  password)).toSeq

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) {
    Some("snapshots" at nexus + "content/repositories/snapshots")
  } else {
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}
