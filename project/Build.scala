import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "CLI"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "fr.greweb" %% "playcli" % "0.1-SNAPSHOT" from /* FIXME */
      Path.userHome.asFile.toURI.toURL+".ivy2/local/fr.greweb/playcli_2.10/0.1-SNAPSHOT/jars/playcli_2.10.jar"
    
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
