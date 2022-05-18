import $ivy.`com.github.bellam::oauth-signature:0.1.1`

import io.bellare.OAuthConfig
import io.bellare.OAuthRequest
import io.bellare.OAuthSignature
import io.bellare.OAuthHelper

@main
def main(args: String*): Unit = {
  println(go(args(0), "-1"))
}

def go(screenName: String, cursor: String): String = {
  Thread.sleep(2000)
  // println(s"**** $cursor ****")

  if (cursor == "0") {
    "Done"
  }
  // 1. create auth header
  val env = OAuthHelper.env
  val config = OAuthConfig(
    oauth_consumer_key = env("TWITTER_CONSUMER_KEY"),
    oauth_consumer_secret = env("TWITTER_CONSUMER_SECRET"),
    oauth_token = env("TWITTER_ACCESS_TOKEN"),
    oauth_token_secret = env("TWITTER_ACCESS_TOKEN_SECRET")
  )

  val request = OAuthRequest(
    http_method = "GET",
    http_url = "https://api.twitter.com/1.1/followers/list.json",
    query_parameters = Map(
      "screen_name" -> screenName,
      "count" -> "200",
      "include_user_entities" -> "false",
      "cursor" -> cursor
    )
  )

  val printer: ujson.Value.Value => Unit = data =>
    data("users").arr.foreach(u => {
      println(
        s"${u("screen_name")}\t${u("created_at")}\t${u("followers_count")}\t${u("friends_count")}\t${u("statuses_count")}"
      )
    })

  // get signed Auth Header
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
    val response: requests.Response = requests.get(
      s"${request.http_url}${queryString(request)}",
      headers = Map(
        "authorization" -> authorizationHeader,
        "content-type" -> "application/json"
      ),
      data = request.query_parameters
    )

    // 4. parse json
    val data = ujson.read(response.text)
    printer(data)

    go(screenName, data("next_cursor_str").str)
  } catch {
    case re: requests.RequestFailedException => {
      println(
        s"Request Failed: Code 429 --> Waiting for 15 mins - $cursor - $re"
      )
      Thread.sleep(900000)
      go(screenName, cursor)
    }
  }
}
