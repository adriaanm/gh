import play.api.libs.ws.{WS, Response}
import play.api.libs.json.{JsValue, Json}

new play.core.StaticApplication(new java.io.File("."))

val milestoneForBranch = Map(
  "2.10.x" -> "2.10.1-RC1",
  "2.10.0-wip" -> "2.10.0-RC3",
  "master" -> "2.11.0-M1",
  "2.9.x" -> "2.9.3-RC1")

import play.api.libs.concurrent.Execution.Implicits._

val milestones =  WS.url("https://api.github.com/repos/scala/scala/milestones").get().map(_.json.as[Seq[JsValue]].map{case ms => ((ms \ "title").as[String], (ms \ "number"))}.toMap)

def makeToken(user: String, pass: String) = for (
    resp <- WS.url("https://api.github.com/authorizations").withAuth(user, pass, com.ning.http.client.Realm.AuthScheme.BASIC).post(Json.toJson(Map("scopes" -> Json.toJson(Seq(Json.toJson("public_repo"))), "note" -> Json.toJson("gh")))))
      yield (resp.json \ "token").as[String]

def assignMilestone(pull: JsValue, milestones: Map[String, JsValue], token: String) = {
  val milestone = (pull \ "milestone").asOpt[String];

  for (
    number <- (pull \ "number").asOpt[Int];
    state  <- (pull \ "state").asOpt[String];
    tgt    <- (pull \ "base" \ "ref").asOpt[String];
    title  <- (pull \ "title").asOpt[String];
      if milestone.isEmpty && milestoneForBranch.isDefinedAt(tgt)) {

    println((state, tgt, milestone, title))
    val milestoneJson = milestones(milestoneForBranch(tgt))
    val body = Json.toJson(Map("milestone" -> milestoneJson))
    println("pull #"+ number +" should be "+ body)
    val f = WS.url("https://api.github.com/repos/scala/scala/issues/"+ number).withHeaders(("Authorization", "token "+ token)).execute("PATCH", body)
    f onFailure{ case res: Response => println("failed: "+ res) }
    f onSuccess{ case res: Response => println("ok: "+ res.statusText + res.body) }
  }
}

for (
  token <- makeToken(YOUR_GITHUB_USERNAME, YOUR_GITHUB_PASSWORD); // it's probably best to do this only once and store the token
  milestones <- milestones;
  pulls <- WS.url("https://api.github.com/repos/scala/scala/pulls?state=open").get();
  pull <- pulls.json.as[Seq[JsValue]])   // pull <- WS.url("https://api.github.com/repos/scala/scala/pulls/1606").get())
    assignMilestone(pull, milestones, token)
