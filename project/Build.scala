import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "CLI"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "fr.greweb" %% "playcli" % "0.11"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
