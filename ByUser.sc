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

def go(handle: String, maxId: String): Unit = {
  Thread.sleep(2000)
  println(s"**** maxId $maxId ****")

  // 1. create auth header
  val env = OAuthHelper.env
  val config = OAuthConfig(
    oauth_consumer_key = env("TWITTER_CONSUMER_KEY"),
    oauth_consumer_secret = env("TWITTER_CONSUMER_SECRET"),
    oauth_token = env("TWITTER_ACCESS_TOKEN"),
    oauth_token_secret = env("TWITTER_ACCESS_TOKEN_SECRET")
  )

  var qp = Map(
    "screen_name" -> handle,
    "count" -> "200",
    "include_rts" -> "1",
    "max_id" -> maxId
  )

  if (maxId == "0") {
    qp = Map(
      "screen_name" -> handle,
      "count" -> "200",
      "include_rts" -> "1"
    )
  }

  val request = OAuthRequest(
    http_method = "GET",
    http_url = "https://api.twitter.com/1.1/statuses/user_timeline.json",
    query_parameters = qp
  )

  val printer: (File, ujson.Value.Value) => String = (file, data) => {
    val bw = new BufferedWriter(new FileWriter(file, true))
    if (data != null) {
      data.arr.foreach(t => {
        bw.write(s"$t\n")
      })
    }

    bw.close()

    val lastId = data.arr.lastOption match {
      case Some(last) => last("id_str").str
      case None       => ""
    }

    return lastId
  }
  val signature = OAuthSignature(config, request)
  val authorizationHeader = signature.getSignedAuthorizationHeader

  // 2. construct query string
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

  // 3. get search response
  try {

    print(request)

    val response: requests.Response = requests.get(
      s"${request.http_url}${queryString(request)}",
      headers = Map(
        "authorization" -> authorizationHeader,
        "content-type" -> "application/json"
      ),
      data = request.query_parameters
    )

    // 4. parse json
    val handleFile = new File(s"data/$handle")
    val data = ujson.read(response.text)
    val lastId = printer(handleFile, data)

    go(handle, lastId)

  } catch {
    case re: requests.RequestFailedException => {
      println(
        s"Done - $maxId"
      )
    }

  }
}
