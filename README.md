Set your Twitter app keys as **environment variables** with following names:

```
TWITTER_CONSUMER_KEY
TWITTER_CONSUMER_SECRET
TWITTER_ACCESS_TOKEN
TWITTER_ACCESS_TOKEN_SECRET
```

This project uses ammonite. Install ammonite command line interface on your machine, visit https://ammonite.io

---

**To get all tweets for a hashtag or search term.**

```
$ amm ByHashtag.sc "#BTS" -1

arguments:
<hashtag> required -- the hashtag / search term
<last_tweet_id> optional  -- tweet id used as cursor to get tweets before this id.
```

This commands writes the output to a file with the hashtag as file name.

**To get all tweets by a user.**

```
$ amm ByUser.sc "@jack" -1

arguments:
<twitter_username> required -- the hashtag / search term
<last_tweet_id> optional  -- tweet id used as cursor to get tweets before this id.
```

This command writes the output to console.

**To get all followers of a user.**

```
$ amm Followers.sc "@jack" -1

arguments:
<twitter_username> required -- the hashtag / search term
<last_follower_id> optional  -- follower user id used as cursor to get users after this id.
```

This command writes the output to console.

---

This project uses Twitter's API v1.1.
