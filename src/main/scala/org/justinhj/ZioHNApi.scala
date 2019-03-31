package org.justinhj

import org.justinhj.httpclient.HttpClient
import scalaz.zio.Runtime
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.{putStrLn, _}
import scalaz.zio.internal.{Platform, PlatformLive}
import scalaz.zio.random.Random
import scalaz.zio.system.System

trait LiveRuntime extends Runtime[Clock with Console with System with Random with Blocking with HttpClient] {
  type Environment = Clock with Console with System with Random with Blocking with HttpClient

  val Platform: Platform       = PlatformLive.Default
  val Environment: Environment = new Clock.Live with Console.Live with System.Live with Random.Live with Blocking.Live
    with HttpClient.Live
}

object ZioHNApi {

  def main(args: Array[String]): Unit = {

    type HNUserID = String
    type HNItemID = Int

    val HNMissingItemID : HNItemID = -1
    val HNMissingUserID : HNUserID = ""

    val baseHNURL : String = "https://hacker-news.firebaseio.com/v0/"
    // These functions construct the url for various api queries
    def getUserURL(userId: HNUserID) = s"${baseHNURL}user/$userId.json"

    def getItemURL(itemId: HNItemID) = s"${baseHNURL}item/$itemId.json"

    val getTopItemsURL = s"${baseHNURL}topstories.json"

    val getMaxItemURL = s"${baseHNURL}maxitem.json"

    val helloWorld = putStrLn(s"There are ${args.length} args");

    val runtime = new LiveRuntime {}

    val program = for(
      _ <- helloWorld;
      s <- httpclient.get(getTopItemsURL);
      _ <- putStrLn(s"Received ${s} from httpclient")
    ) yield s

    runtime.unsafeRun(program)

  }


}
