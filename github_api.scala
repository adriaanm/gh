import play.api.libs.ws.{WS, Response}
import play.api.libs.json.{JsValue, Json, JsNull}

object mergeTargetToMilestone {
  val app = new play.core.StaticApplication(new java.io.File("."))

  val milestoneForBranch = Map(
    "2.10.x" -> "2.10.1-RC1",
    "2.10.0-wip" -> "2.10.0-RC3",
    "master" -> "2.11.0-M1",
    "2.9.x" -> "2.9.3-RC1")

  import play.api.libs.concurrent.Execution.Implicits._

  val milestones =  WS.url("https://api.github.com/repos/scala/scala/milestones").get().map(_.json.as[Seq[JsValue]].map{case ms => ((ms \ "title").as[String], (ms \ "number"))}.toMap)

  import play.api.libs.concurrent.Execution.Implicits._

  def makeToken(user: String, pass: String) = for (
      resp <- WS.url("https://api.github.com/authorizations").withAuth(user, pass, com.ning.http.client.Realm.AuthScheme.BASIC).post(Json.toJson(Map("scopes" -> Json.toJson(Seq(Json.toJson("public_repo"))), "note" -> Json.toJson("gh")))))
        yield (resp.json \ "token").as[String]

  def assignMilestone(pull: JsValue, milestones: Map[String, JsValue], token: String) = {
    val milestone = (pull \ "milestone")
    // println("MS: "+ (milestone == JsNull, (pull \ "title").asOpt[String], (pull \ "base" \ "ref").asOpt[String], (pull \ "state").asOpt[String], (pull \ "number").asOpt[Int]))
    for (
      number <- (pull \ "number").asOpt[Int];
      state  <- (pull \ "state").asOpt[String];
      tgt    <- (pull \ "base" \ "ref").asOpt[String];
      title  <- (pull \ "title").asOpt[String];
        if milestone == JsNull && milestoneForBranch.isDefinedAt(tgt)) {

      // println((state, tgt, milestone, title))

      val milestoneName = milestoneForBranch(tgt)
      val milestoneNumber = milestones(milestoneName)
      val body = Json.toJson(Map("milestone" -> milestoneNumber))

      val f = WS.url("https://api.github.com/repos/scala/scala/issues/"+ number).withHeaders(("Authorization", "token "+ token)).execute("PATCH", body)
      f onFailure{ case res: Response => println(s"FAILED: #$number --> milestone $milestoneName (${res.statusText})") }
      f onSuccess{ case res: Response => println(s"#$number --> milestone $milestoneName (${res.statusText})") }
      f
    }
  }

  // usage: Play2.1/play run-main mergeTargetToMilestone $TOKEN
  // token can be computed by running makeToken($user, $pass) once in a play console
  def main(args: Array[String]) = {
    val token = args(0)

    // for ( _ <-
    for (
      // token <- makeToken(args(1), args(2)); // it's probably best to do this only once and store the token
      milestones <- milestones;
      pulls  <- WS.url("https://api.github.com/repos/scala/scala/pulls?state=open").get();
      pull   <- pulls.json.as[Seq[JsValue]] ) {
        assignMilestone(pull, milestones, token)
      }
    // ) { Play.stop() }
  }
}