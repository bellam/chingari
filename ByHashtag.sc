import $ivy.`com.github.bellam::oauth-signature:0.1.1`

import io.bellare.OAuthConfig
import io.bellare.OAuthRequest
import io.bellare.OAuthSignature
import io.bellare.OAuthHelper

import java.io._

@main
def main(s: String*): Unit = {
  go(s(0), s(1))
}

def go(hashtag: String, maxId: String): Unit = {

  val env = OAuthHelper.env
  val config = OAuthConfig(
    oauth_consumer_key = env("TWITTER_CONSUMER_KEY"),
    oauth_consumer_secret = env("TWITTER_CONSUMER_SECRET"),
    oauth_token = env("TWITTER_ACCESS_TOKEN"),
    oauth_token_secret = env("TWITTER_ACCESS_TOKEN_SECRET")
  )

  var qp = Map(
    "q" -> hashtag,
    "count" -> "100",
    "result_type" -> "recent",
    "max_id" -> maxId
  )
  if (maxId == "0")
    qp = Map("q" -> hashtag, "count" -> "100", "result_type" -> "recent")

  val file = new File(s"data/$hashtag")

  val printer: (File, ujson.Value.Value) => String = (file, data) => {
    val bw = new BufferedWriter(new FileWriter(file, true))
    data("statuses").arr.foreach(t => {
      val u = t("user")

      val count: Double = u("statuses_count").num
      val created: String = u("created_at").str

      val text =
        s"${t("id")}\t${t("created_at")}\t${u("screen_name")}\t${u(
            "created_at"
          )}\t${u("statuses_count")}\t${u("followers_count")}\t${u(
            "friends_count"
          )}\t${t("retweeted")}\t${u("verified")}\t${t("text")}\t${t("source")}\n"

      bw.write(text)
    })
    bw.close()

    val lastId = data("statuses").arr.lastOption match {
      case Some(last) => last("id_str").str
      case None       => ""
    }

    lastId

  }

  var scrape: Boolean = true
  var lastId: String = maxId
  while (scrape) {
    val request = OAuthRequest(
      http_method = "GET",
      http_url = "https://api.twitter.com/1.1/search/tweets.json",
      query_parameters = qp
    )
    val signature = OAuthSignature(config, request)
    val authorizationHeader = signature.getSignedAuthorizationHeader
    val queryString: OAuthRequest => String = request =>
      request.http_method match {
        case "GET" =>
          ("?") +
            request.query_parameters
              .map(e => e._1 + "=" + OAuthHelper.encode(e._2.toString))
              .toList
              .sorted
              .reduce((x, y) => x + "&" + y)
        case _ => ""
      }

    try {
      val response: requests.Response = requests.get(
        s"${request.http_url}${queryString(request)}",
        headers = Map(
          "authorization" -> authorizationHeader,
          "content-type" -> "application/json"
        ),
        data = request.query_parameters
      )

      val data = ujson.read(response.text)
      if (data("statuses").arr.length < 2) {
        scrape = false
      }
      lastId = printer(file, data)
      println(s"--- $lastId ---")

    } catch {
      case re: requests.RequestFailedException => {
        println(
          s"Done - $hashtag - $re"
        )
        Thread.sleep(900000)
      }
      case e => println(e)
    } finally {
      qp = Map(
        "q" -> hashtag,
        "count" -> "100",
        "result_type" -> "recent",
        "max_id" -> lastId
      )
    }

  }

}
