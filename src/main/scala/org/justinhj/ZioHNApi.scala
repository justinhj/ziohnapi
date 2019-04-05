package org.justinhj

import java.io.IOException

import org.justinhj.httpclient.HttpClient
import scalaz.zio.{Runtime, ZIO}
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.{putStrLn, _}
import scalaz.zio.internal.{Platform, PlatformLive}
import scalaz.zio.random.Random
import scalaz.zio.system.System
import upickle.default.read

trait LiveRuntime extends Runtime[Clock with Console with System with Random with Blocking with HttpClient] {
  type Environment = Clock with Console with System with Random with Blocking with HttpClient

  val Platform: Platform       = PlatformLive.Default
  val Environment: Environment = new Clock.Live with Console.Live with System.Live with Random.Live with Blocking.Live
    with HttpClient.HttpClientLive
}

object ZioHNApi {

  type HNUserID = String
  type HNItemID = Int

  val HNMissingItemID : HNItemID = -1
  val HNMissingUserID : HNUserID = ""

  type HNItemIDList = List[HNItemID]

  val baseHNURL : String = "https://hacker-news.firebaseio.com/v0/"
  // These functions construct the url for various api queries
  def getUserURL(userId: HNUserID) = s"${baseHNURL}user/$userId.json"

  def getItemURL(itemId: HNItemID) = s"${baseHNURL}item/$itemId.json"

  val getTopItemsURL = s"${baseHNURL}topstories.json"

  val getMaxItemURL = s"${baseHNURL}maxitem.json"

  def parseTopItemsResponse(s: String): HNItemIDList = {
    val result: HNItemIDList = read[HNItemIDList](s)
    result
  }

  def main(args: Array[String]): Unit = {

    val runtime = new LiveRuntime {}

    val helloWorld = putStrLn(s"There are ${args.length} args")
    val readWorld: ZIO[Console, IOException, String] = getStrLn

    val program = for(
      _ <- helloWorld;
      s <- httpclient.get(getMaxItemURL);
      items = parseTopItemsResponse(s);
      _ <- putStrLn(s"Received ${items.size} top page items from httpclient")
    ) yield ()

    val handleErrors = program.foldM(err => putStrLn(s"Failed with ${err.getMessage}"), _ => putStrLn("Success"))

    runtime.unsafeRunSync(handleErrors)
  }

}
