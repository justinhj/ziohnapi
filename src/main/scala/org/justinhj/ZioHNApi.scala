package org.justinhj

import java.io.IOException

import org.justinhj.httpclient.HttpClient
import org.justinhj.util.Util
import scalaz.zio.{Runtime, Task, UIO, ZIO}
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.{putStrLn, _}
import scalaz.zio.internal.{Platform, PlatformLive}
import scalaz.zio.random.Random
import scalaz.zio.system.System
import upickle.default._

trait LiveRuntime extends Runtime[Clock with Console with System with Random with Blocking with HttpClient] {
  type Environment = Clock with Console with System with Random with Blocking with HttpClient

  val Platform: Platform       = PlatformLive.Default
  val Environment: Environment = new Clock.Live with Console.Live with System.Live with Random.Live with Blocking.Live
    with HttpClient.HttpClientLive
}

object ZioHNApi {

  type HNUserID = String
  type HNItemID = Int

  // Hacker news API represents missing Id's in their output as follows
  val HNMissingItemID : HNItemID = -1
  val HNMissingUserID : HNUserID = ""

  type HNItemIDList = List[HNItemID]

  val baseHNURL : String = "https://hacker-news.firebaseio.com/v0/"

  // These functions construct the url for various api queries
  def getUserURL(userId: HNUserID) = s"${baseHNURL}user/$userId.json"
  def getItemURL(itemId: HNItemID) = s"${baseHNURL}item/$itemId.json"

  val getTopItemsURL = s"${baseHNURL}topstories.json"
  val getMaxItemURL = s"${baseHNURL}maxitem.json"

  case class HNItem(
     id : HNItemID, // The item's unique id.
     deleted : Option[Boolean] = Some(false), // true if the item is deleted.
     `type` : String, // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
     by : HNUserID = HNMissingUserID, // The username of the item's author.
     time : Int, // Creation date of the item, in Unix Time.
     text : String = "", // The comment, story or poll text. HTML.
     dead : Option[Boolean] = Some(false), // true if the item is dead.
     parent : HNItemID = HNMissingItemID, // The comment's parent: either another comment or the relevant story.
     poll : Option[HNItemID] = Some(HNMissingItemID), // The pollopt's associated poll.
     kids : Option[List[HNItemID]] = Some(List.empty), // The ids of the item's comments, in ranked display order.
     url : Option[String] = Some(""), // The URL of the story.
     score : Option[Int] = Some(-1), // The story's score, or the votes for a pollopt.
     title : Option[String] = Some(""), // The title of the story, poll or job.
     parts : Option[List[HNItemID]] = Some(List.empty), // A list of related pollopts, in display order.
     descendants : Option[Int] = Some(0) // In the case of stories or polls, the total comment count.
   )

  // Parse a type T form the input string, throws
  def parseHNResponse[T](s: String)(implicit r: Reader[T]) : Task[T] = {
    ZIO.effect(read[T](s))
  }

  def parseTopItemsResponse(s: String): Task[HNItemIDList]= parseHNResponse[HNItemIDList](s)

  def parseItemResponse(s: String) : Task[HNItem] = parseHNResponse[HNItem](s)

  // Simple input and output interaction
  def promptInput = UIO {
    println("Enter a page number to fetch a page of news items or anything else to quit: ")
  }

  def getNumericInput = {
    (for (
      input <- getStrLn;
      num <- Task.effect(input.toInt)
    ) yield num).fold(err => None, succ => Some(succ))
  }

  def printTopItemCount(topItems: HNItemIDList) =
    putStrLn(s"Got ${topItems.size} items")

  def printError(err: String) =
    putStrLn(s"Error: $err")

  def getUserPage = for (
    _ <- promptInput;
    page <- getNumericInput
  ) yield page

  def fetchItem(id: HNItemID) = {
    for (
      s <- httpclient.get(getItemURL(id));
      item <- parseItemResponse(s)
    ) yield item
  }

  def fetchPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList) = {

    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    ZIO.foreachParN(4)(pageOfItems){
      id =>
        fetchItem(id)
    }

//    val fetchItems = pageOfItems.traverse(HNDataSources.getItem)
//
//    cache match {
//      case Some(c) =>
//        fetchItems.runF[Task](c)
//      case None =>
//        fetchItems.runF[Task]
//    }

  }

  // Print a page of fetched items
  def printPageItems(startPage: Int, numItemsPerPage: Int, items: List[HNItem]) = {
    // helper to show the article rank
    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1

    ZIO.effect {

      items.zipWithIndex.foreach {
        case (item, n) =>
          println(s"${itemNum(n)}. ${item.title} ${Util.getHostName(item.url.getOrElse("none"))}")
          println(s"  ${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} ${item.descendants} comments\n")
      }
    }
  }
  def showPagesLoop(topItems: HNItemIDList) : ZIO[Console with HttpClient, Throwable, Unit] = {

    getUserPage.flatMap {
      case Some(pageNumber) =>
        for(
          _ <- putStrLn(s"Page $pageNumber");
          items <- fetchPage(pageNumber, 10, topItems);
          _ <- printPageItems(pageNumber, 10, items);
          _ <- showPagesLoop(topItems)
        ) yield ()
      case None =>
        putStrLn("Have a nice day!")
    }

//  // Here we will show the page of items or exit if the user didn't enter a number
//    getUserPage.flatMap {
//
//      case Some(page) =>
//        println(s"fetch page $page")
//
//        for (
//          fetchResult <- fetchPage(page, numItemsPerPage, topItems, cache);
//          (env, items) = fetchResult;
//          _ = println(s"${env.rounds.size} fetch rounds");
//          _ <- printPageItems(page, numItemsPerPage, items);
//          newCache <- showPagesLoop(topItems, Some(env.cache))
//        ) yield newCache
//
//
//      case None =>
//        Task.succeed(())
    }


  def main(args: Array[String]): Unit = {

    val runtime = new LiveRuntime {}

    val startMessage = putStrLn(s"There are ${args.length} args")

    val frontPage = for (
      s <- httpclient.get(getTopItemsURL);
      items <- parseTopItemsResponse(s);
      _ <- showPagesLoop(items)
    ) yield ()

    runtime.unsafeRunSync(frontPage)


    val program = for(
      _ <- startMessage;
      s <- httpclient.get(getTopItemsURL);
      // This is short form of the following (httpclient is implemented in the package object)
      //s <- ZIO.accessM[HttpClient with Blocking, Throwable, String](_.httpClient get getTopItemsURL);
      items <- parseTopItemsResponse(s);
      _ <- putStrLn(s"Received ${items.size} top page items from httpclient")
    ) yield ()

    val handleErrors = program.foldM(
      err =>
        putStrLn(s"Failed with ${err.getMessage}"),
      _ =>
        putStrLn("Success"))

    runtime.unsafeRunSync(handleErrors)
  }

}
