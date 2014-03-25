import sbt._
import Keys._

object Properties {
  lazy val scalaVer = "2.10.3"
}

object Resolvers {
  lazy val typesafeReleases = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
  lazy val scalaToolsRepo   = "sonatype-oss-public" at "https://oss.sonatype.org/content/groups/public/"
}

object BuildSettings {
  import Properties._
  lazy val buildSettings = Defaults.defaultSettings ++ Seq (
    organization        := "com.eligotech",
    crossScalaVersions  := Seq("2.10.3"),
    sbtPlugin           := true,
    version             := "0.4",
    scalaVersion        := scalaVer,
    scalacOptions       := Seq("-unchecked", "-deprecation"),
    //doesn't work
    ivyValidate := false
  )
}

object PublishSettings {
  val publishSettings = Seq(
    publishTo <<= version { (v: String) =>
      val nexus = "http://repo.eligotech.com/nexus/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "content/repositories/releases")
    },
    credentials += Credentials(Path.userHome / ".sbt" / "sonatype.credentials"),
    publishMavenStyle := true
  )
}


object ApplicationBuild extends Build {
  import Resolvers._
  import BuildSettings._

  lazy val sbt_dist = Project(
    "sbt-dist",
    file("."),
    settings =  buildSettings ++
      Seq(resolvers ++= Seq(typesafeReleases, scalaToolsRepo))
  )
  .settings( PublishSettings.publishSettings:_* )
}
