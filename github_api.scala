import play.api.libs.ws.{WS, Response}
import play.api.libs.json.{JsValue, Json, JsNull}

// TODO: look at actual merge target and depending on time assign the right milestone
/*
import org.eclipse.jgit._
import revwalk.RevWalk
import revwalk.filter._
import collection.JavaConverters._

val tickets = Array("SI-1987", "SI-2296", "SI-3943", "SI-4478", "SI-4581", "SI-4813", "SI-4857", "SI-5418", "SI-5692", "SI-5762", "SI-5767", "SI-5770", "SI-5788", "SI-5822", "SI-5918", "SI-5942", "SI-5943", "SI-6034", "SI-6052", "SI-6114", "SI-6162", "SI-6170", "SI-6189", "SI-6190", "SI-6220", "SI-6227", "SI-6236", "SI-6245", "SI-6246", "SI-6258", "SI-6259", "SI-6260", "SI-6263", "SI-6264", "SI-6268", "SI-6271", "SI-6272", "SI-6273", "SI-6274", "SI-6275", "SI-6276", "SI-6277", "SI-6278", "SI-6280", "SI-6281", "SI-6283", "SI-6285", "SI-6287", "SI-6290", "SI-6293", "SI-6294", "SI-6305", "SI-6306", "SI-6310", "SI-6311", "SI-6318", "SI-6323", "SI-6327", "SI-6329", "SI-6331", "SI-6333", "SI-6335", "SI-6336", "SI-6337", "SI-6340", "SI-6342", "SI-6344", "SI-6345", "SI-6353", "SI-6354", "SI-6356", "SI-6359", "SI-6360", "SI-6362", "SI-6363", "SI-6365", "SI-6367", "SI-6369", "SI-6371", "SI-6372", "SI-6373", "SI-6374", "SI-6380", "SI-6385", "SI-6390", "SI-6392", "SI-6394", "SI-6409", "SI-6410", "SI-6412", "SI-6417", "SI-6442", "SI-6449")

val repo   = (new storage.file.FileRepositoryBuilder).setGitDir(new java.io.File("/Users/moors/git/scala/.git")).findGitDir.build
val branch = repo.resolve("2.10.0-wip")
val walk   = new RevWalk(repo)

val after = CommitTimeRevFilter.after(new java.util.Date("8/15/2012"))


def commitsFor(ticket: String) = {
  walk.reset()
  walk.markStart(walk.parseCommit(branch))

  val msg    = MessageRevFilter.create(ticket)
  val filter = AndRevFilter.create(msg, after)

  walk.setRevFilter(filter)

  walk.iterator.asScala.map(c => (c.getId, c.getFullMessage)).toList
}

val commitsReferencingTicket = tickets map (t => (t, commitsFor(t))) toMap

def display(commit: revwalk.RevCommit) = {
  println(new java.util.Date(commit.getCommitTime * 1000L)+"\n"+commit.getFullMessage)
}

*/

object mergeTargetToMilestone {
  val app = new play.core.StaticApplication(new java.io.File("."))

  // val milestoneForBranch = Map(
  //   "2.10.x" -> "2.10.1-RC1",
  //   "2.10.0-wip" -> "2.10.0-RC3",
  //   "master" -> "2.11.0-M1",
  //   "2.9.x" -> "2.9.3-RC1")

  def milestoneForBranch(ref: String): Option[String] = {
    println("milestoneForBranch: "+ ref)
    LocalScalaRepo.closestTagsOfRef(ref).headOption
  }

  import play.api.libs.concurrent.Execution.Implicits._

  val milestones =  WS.url("https://api.github.com/repos/scala/scala/milestones").get().map(_.json.as[Seq[JsValue]].map{case ms => ((ms \ "title").as[String], (ms \ "number"))}.toMap)


  def makeToken(user: String, pass: String) = for (
      resp <- WS.url("https://api.github.com/authorizations").withAuth(user, pass, com.ning.http.client.Realm.AuthScheme.BASIC).post(Json.toJson(Map("scopes" -> Json.toJson(Seq(Json.toJson("public_repo"))), "note" -> Json.toJson("gh")))))
        yield (resp.json \ "token").as[String]

  def assignMilestone(pull: JsValue, milestones: Map[String, JsValue], token: String) = {
    val milestone = (pull \ "milestone")
    // println("MS: "+ (milestone == JsNull, (pull \ "title").asOpt[String], (pull \ "base" \ "ref").asOpt[String], (pull \ "state").asOpt[String], (pull \ "number").asOpt[Int]))
    for (
      number <- (pull \ "number").asOpt[Int];
      state  <- (pull \ "state").asOpt[String];
      tgt    <- (pull \ "base" \ "sha").asOpt[String];
      title  <- (pull \ "title").asOpt[String];
        if milestone == JsNull;
      milestoneName <- milestoneForBranch(tgt)) {

      // println((state, tgt, milestone, title))

      println(s"milestone $milestoneName for #$number $title (based on $tgt)")

      // val milestoneName = milestoneForBranch(tgt)
      // val milestoneNumber = milestones(milestoneName)
      // val body = Json.toJson(Map("milestone" -> milestoneNumber))
      //
      // val f = WS.url("https://api.github.com/repos/scala/scala/issues/"+ number).withHeaders(("Authorization", "token "+ token)).execute("PATCH", body)
      // f onFailure{ case res: Response => println(s"FAILED: #$number --> milestone $milestoneName (${res.statusText})") }
      // f onSuccess{ case res: Response => println(s"#$number --> milestone $milestoneName (${res.statusText})") }
      // f
    }
  }

  def iterateJsonPages(req: WS.WSRequestHolder, iteratee: Iteratee[JsValue, Unit]): Unit = {
    def parseNext(link: String): Option[String] = None

    // while there's a Link header with a rel="next" url, fetch that url and present its body as part of the current request
    req.get().flatMap { resp =>
      parseNext(resp.header("Link")) match {
        case Some(next) => ??? // return the future that concatenates
        case None => resp.json.as[Seq[JsValue]].foreach
      }
    }
  }

  // usage: Play2.1/play run-main mergeTargetToMilestone $TOKEN
  // token can be computed by running makeToken($user, $pass) once in a play console
  def main(args: Array[String]) = {
    val token = args(0)

    iterateJsonPages(
      WS.url("https://api.github.com/repos/scala/scala/issues?labels=&milestone=none&state=closed",
      Iteratee.foreach(pull => assignMilestone(pull, milestones, token)))
    )

    // for ( _ <-
    // for (
      // token <- makeToken(args(1), args(2)); // it's probably best to do this only once and store the token
      // milestones <- milestones; // https://github.com/scala/scala/issues?labels=&milestone=none&page=1&state=closed
      // TODO: detect pagination and request all pages
      // there will be a header like this:
      //  Link: <https://api.github.com/repos/scala/scala/issues?labels=&milestone=none&page=2&state=closed>; rel="next", <https://api.github.com/repos/scala/scala/issues?labels=&milestone=none&page=47&state=closed>; rel="last"

    // ) { Play.stop() }
  }
}