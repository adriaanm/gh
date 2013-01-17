import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "gh2"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "2.1.0.201209190230-r"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"
  )

}
