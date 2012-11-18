import org.eclipse.jgit._
import revwalk.RevWalk
import revwalk.filter._
import collection.JavaConverters._
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId

// val tickets = Array("SI-1987", "SI-2296", "SI-3943", "SI-4478", "SI-4581", "SI-4813", "SI-4857", "SI-5418", "SI-5692", "SI-5762", "SI-5767", "SI-5770", "SI-5788", "SI-5822", "SI-5918", "SI-5942", "SI-5943", "SI-6034", "SI-6052", "SI-6114", "SI-6162", "SI-6170", "SI-6189", "SI-6190", "SI-6220", "SI-6227", "SI-6236", "SI-6245", "SI-6246", "SI-6258", "SI-6259", "SI-6260", "SI-6263", "SI-6264", "SI-6268", "SI-6271", "SI-6272", "SI-6273", "SI-6274", "SI-6275", "SI-6276", "SI-6277", "SI-6278", "SI-6280", "SI-6281", "SI-6283", "SI-6285", "SI-6287", "SI-6290", "SI-6293", "SI-6294", "SI-6305", "SI-6306", "SI-6310", "SI-6311", "SI-6318", "SI-6323", "SI-6327", "SI-6329", "SI-6331", "SI-6333", "SI-6335", "SI-6336", "SI-6337", "SI-6340", "SI-6342", "SI-6344", "SI-6345", "SI-6353", "SI-6354", "SI-6356", "SI-6359", "SI-6360", "SI-6362", "SI-6363", "SI-6365", "SI-6367", "SI-6369", "SI-6371", "SI-6372", "SI-6373", "SI-6374", "SI-6380", "SI-6385", "SI-6390", "SI-6392", "SI-6394", "SI-6409", "SI-6410", "SI-6412", "SI-6417", "SI-6442", "SI-6449")

object LocalScalaRepo {
  val repo   = (new storage.file.FileRepositoryBuilder).setGitDir(new java.io.File("/Users/adriaan/git/scala/.git")).findGitDir.build
  // val branch = repo.resolve("2.10.0-wip")
  // val walk   = new RevWalk(repo)
  // 
  // val after = CommitTimeRevFilter.after(new java.util.Date("8/15/2012"))

  // def commitsFor(ticket: String, branch: ObjectId) = {
  //   walk.reset()
  //   walk.markStart(walk parseCommit branch)
  // 
  //   val msg    = MessageRevFilter.create(ticket)
  //   val filter = AndRevFilter.create(msg, after)
  // 
  //   walk.setRevFilter(filter)
  // 
  //   walk.iterator.asScala.map(c => (c.getId, c.getFullMessage)).toList
  // }

  // val commitsReferencingTicket = tickets map (t => (t, commitsFor(t))) toMap
  // 
  // def display(commit: revwalk.RevCommit) = {
  //   println(new java.util.Date(commit.getCommitTime * 1000L)+"\n"+commit.getFullMessage)
  // }


  def closestTagsOfRef(ref: String) = {
    val oid = repo resolve ref
    val tagLookup = collection.mutable.HashMap[revwalk.RevCommit, List[String]]() withDefaultValue(Nil)

    val walk = new RevWalk(repo)
    val WantedFlag = walk.newFlag("wanted");

    for (
      tag <- repo.getTags.values.asScala;
      commit = walk parseCommit (tag.getObjectId);
        if (commit.getType == Constants.OBJ_COMMIT)) {

      commit add WantedFlag

      tagLookup(commit) ::= tag.getName.split("/").last
    }

    walk markStart    (walk parseCommit oid)
    walk setRevFilter RevFilter.ALL
    walk sort         revwalk.RevSort.TOPO

    walk.iterator.asScala.find(_ has WantedFlag).map(tag => tagLookup(walk parseCommit tag.getId)).getOrElse(Nil)
  }
}