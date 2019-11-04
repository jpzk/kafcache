
lazy val commonSettings = Seq(
  organization := "com.madewithtea",
  version := "1.3.0",
  scalaVersion := "2.12.8",
  description := "Memcached and Redis for Kafka Streams",
  organizationHomepage := Some(url("https://www.madewithtea.com")),
  scalacOptions := Seq("-Xexperimental"))

val scalaTestVersion = "3.0.8"
val rocksDBVersion = "5.18.3"
val kafkaVersion = "2.3.0"

lazy val dependencies = Seq(
  "org.apache.kafka" % "kafka-streams" % kafkaVersion,
  "org.apache.kafka" %% "kafka-streams-scala" % kafkaVersion,
  "com.github.cb372" %% "scalacache-memcached" % "0.28.0",
  "commons-codec" % "commons-codec" % "1.13"
)

lazy val test = Seq("org.scalatest" %% "scalatest" % scalaTestVersion % "test")

lazy val kafcache = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= dependencies ++ test
  )

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
  <url>https://kafcache.madewithtea.com</url>
    <licenses>
      <license>
        <name>Apache License Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>jpzk</id>
        <name>Jendrik Poloczek</name>
        <url>https://www.madewithtea.com</url>
      </developer>
    </developers>


micrositeName := "Kafcache"
micrositeDescription := "Memcached and Redis for Kafka Streams"
micrositeUrl := "http://kafcache.madewithtea.com"
micrositeDocumentationUrl := "/docs"
micrositeGitterChannel := false
micrositeDocumentationLabelDescription := "Documentation"
micrositeDataDirectory := (resourceDirectory in Compile).value / "docs" / "data"
micrositeGithubOwner := "jpzk"
micrositeGithubRepo := "kafcache"
micrositeAuthor := "Jendrik Poloczek"
micrositeTwitter := "@madewithtea"
micrositeTwitterCreator := "@madewithtea"
micrositeCompilingDocsTool := WithMdoc
micrositeShareOnSocial := true

lazy val docs = project       // new documentation project
  .in(file("ms-docs")) // important: it must not be docs/
  .dependsOn(kafcache)
  .enablePlugins(MdocPlugin)

enablePlugins(MicrositesPlugin)
