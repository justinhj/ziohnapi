package org.justinhj.hnapi

import org.justinhj.httpclient
import org.justinhj.httpclient.HttpClient
import org.justinhj.util.Util
import scalaz.zio.blocking.Blocking
import scalaz.zio.console.{putStrLn, _}
import scalaz.zio.{Task, UIO, ZIO}
import upickle.default.{ReadWriter, macroRW, _}
import org.apache.commons.text.StringEscapeUtils

object HNApi {

  type Env = HttpClient with Blocking with Console

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
                     deleted : Boolean = false, // true if the item is deleted.
                     `type` : String, // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
                     by : HNUserID = HNMissingUserID, // The username of the item's author.
                     time : Int, // Creation date of the item, in Unix Time.
                     text : String = "", // The comment, story or poll text. HTML.
                     dead : Boolean = false, // true if the item is dead.
                     parent : HNItemID = HNMissingItemID, // The comment's parent: either another comment or the relevant story.
                     poll : HNItemID = HNMissingItemID, // The pollopt's associated poll.
                     kids : List[HNItemID] = List.empty, // The ids of the item's comments, in ranked display order.
                     url : String = "", // The URL of the story.
                     score : Int = 0, // The story's score, or the votes for a pollopt.
                     title : String = "", // The title of the story, poll or job.
                     parts : List[HNItemID] = List.empty, // A list of related pollopts, in display order.
                     descendants : Int = 0 // In the case of stories or polls, the total comment count.
                   )

  object HNItem {
    implicit val rw: ReadWriter[HNItem] = macroRW
  }

  // Parse a type T form the input string, throws
  def parseHNResponse[T](s: String)(implicit r: ReadWriter[T]): ZIO[Console, Throwable, T] = {
    //putStrLn(s"Parsing $s") *>
    ZIO.effect(read[T](s))
  }

  def parseTopItemsResponse(s: String): ZIO[Console, Throwable, HNItemIDList]= parseHNResponse[HNItemIDList](s)
  def parseItemResponse(s: String) : ZIO[Console, Throwable, HNItem] = parseHNResponse[HNItem](s)

  // Simple input and output interaction
  def promptInput: ZIO[Console, Nothing, Unit] = putStrLn("Enter a page number to fetch a page of news items or anything else to quit: ")

  def getNumericInput: ZIO[Console, Nothing, Option[HNItemID]] = {
    (for (
      input <- getStrLn;
      num <- Task.effect(input.toInt)
    ) yield num).fold(err => None, succ => Some(succ))
  }

  def printTopItemCount(topItems: HNItemIDList): ZIO[Console, Nothing, Unit] =
    putStrLn(s"Got ${topItems.size} items")

  def printError(err: String): ZIO[Console, Nothing, Unit] =
    putStrLn(s"Error: $err")

  def getUserPage: ZIO[Console, Nothing, Option[HNItemID]] = for (
    _ <- promptInput;
    page <- getNumericInput
  ) yield page

  def fetchItem(id: HNItemID): ZIO[Env, Throwable, HNItem] = {
    for (
      //_ <- putStrLn(s"fetching $id");
      s <- httpclient.get(getItemURL(id));
      item <- parseItemResponse(s)
    ) yield item
  }

  def fetchPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList): ZIO[Env, Throwable, List[HNItem]] = {
    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)
    ZIO.foreachParN(4)(pageOfItems){id => fetchItem(id)}
  }

  def showComment(item: HNItem): ZIO[Console, Nothing, Unit] = {
    val text = StringEscapeUtils.unescapeHtml3(item.text)
    putStrLn(
      s"""${item.by} ${Util.timestampToPretty(item.time)}
         |${text}""".stripMargin)
  }

  // Print a page of fetched items
  def printPageItems(startPage: Int, numItemsPerPage: Int, items: List[HNItem]): ZIO[Console, Throwable, List[Unit]] = {
    // helper to show the article rank
    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1

    val printList = items.zipWithIndex

    ZIO.foreach(printList){
      case (item, n) =>
        putStrLn(s"${itemNum(n)}. ${item.title} ${Util.getHostName(item.url)} [${item.url}]") *>
          putStrLn(s"  ${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} | ${item.descendants} comments")
    }
  }

  def showPagesLoop(topItems: HNItemIDList) : ZIO[Env, Throwable, Unit] = {

    val itemsPerPage = 5

    getUserPage.flatMap {
      case Some(pageNumber) =>
        for(
          _ <- putStrLn(s"Page $pageNumber");
          items <- fetchPage(pageNumber, itemsPerPage, topItems);
          _ <- printPageItems(pageNumber, itemsPerPage, items);
          _ <- showPagesLoop(topItems)
        ) yield ()
      case None =>
        putStrLn("Have a nice day!")
    }
  }

}
