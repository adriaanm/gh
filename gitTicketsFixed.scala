import scala.sys.process._

class ParseCommits {
  val from = "scala/2.10.x"
  val to = "scala/master"
  val commitToTitle = s"git --no-pager log $from..$to --oneline --no-merges".!!.split("\n").map(_.split(" ", 2) match { case Array(c, t) => (c, t)}).toMap

  // get all the SI references in each commit title
  val commitToSIs = commitToTitle.map{case (k, v) => (k, """SI-\d*""".r.findAllIn(v).toList)}

  // make the reverse mapping
  val SIToCommit = collection.mutable.HashMap[String, String]()
  commitToSIs foreach { case (k, v) => v foreach (SIToCommit += _ -> k) }

  // map SI-XXXX to closest tag where it was fixed
  SIToCommit.map{case (k, v) => (k, (s"git describe $v").!!)}

  // i have a local version of the jira database (https://github.com/adriaanm/bbj)
  // this generates a staged bash script that uses a python cli for jira
  // def fixVersionAnd(i: Int, fv: Int) = {
  //   val newFixVersions = issues.find(_.key== s"SI-$i").map(_.fields("fixVersions").asInstanceOf[List[Version]].map(_.name).map(name2id))
  //   newFixVersions foreach {newFixVersions => println(s"jira update SI-$i fixVersions ${(fv :: newFixVersions).mkString(",")}")}
  // }
}