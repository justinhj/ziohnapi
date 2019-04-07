package org.justinhj

import org.justinhj.ZioHNApi.{HNItem, HNItemIDList, getTopItemsURL}
import org.justinhj.httpclient.HttpClient
import org.justinhj.httpclient.HttpClient.Service
import org.scalatest.FlatSpec
import scalaz.zio.{Exit, IO, Runtime, ZIO}
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalaz.zio.internal.{Platform, PlatformLive}
import scalaz.zio.random.Random
import scalaz.zio.system.System


// Test suite for ZIO HackerNews API
class ZioHNApiTest extends FlatSpec {

  // The test http runtime
  trait HttpClientTest extends HttpClient {

    val sampleTopStories = "[19536375,19536173,19535059,19535564]"
    val sampleItem = """{"by":"justinhj","id":11498534,"parent":11498393,"text":"I&#x27;m an emacs user but I often use a GUI IDE when it makes sense. I find I&#x27;d rather use JetBrains products (PHPStorm, PyCharm and IntelliJ) even though the actual text editing part is nowhere near as powerful and efficient as my Emacs setup.  Sometimes if I need to do some repetitive or monumental editing task that I could script in emacs or use the macro system, I&#x27;ll switch over for a moment. The benefits of being in an environment with a debugger and many other tools as well as project management and auto-completion that is consistent across the languages I use outweigh the cons of leaving emacs. \nI&#x27;m considering switching to Visual Studio just for a React Native project I&#x27;m working on because it seems to handle Javascript&#x2F;JMX much better than anything else I&#x27;ve tried.","time":1460653869,"type":"comment"}"""

    val httpClient: Service[Any with HttpClient with Blocking] = new Service[Any with HttpClient with Blocking] {

      def requestSync(url: String) : String = {
        if(url == ZioHNApi.getTopItemsURL) sampleTopStories
        else if(url == ZioHNApi.getItemURL(11498534)) sampleItem
        else throw new Exception("Not found")
      }

      final def get(url: String) : IO[Throwable, String] = {
        ZIO.effect(requestSync(url))
      }
    }
  }

  object HttpClientTest extends HttpClientTest

  trait TestRuntime extends Runtime[Clock with Console with System with Random with Blocking with HttpClient] {
    type Environment = Clock with Console with System with Random with Blocking with HttpClient

    val Platform: Platform       = PlatformLive.Default
    val Environment: Environment = new Clock.Live with Console.Live with System.Live with Random.Live with Blocking.Live
      with HttpClientTest
  }

  "HNItem" should "parse correctly" in {

    val runtime = new TestRuntime {}

    // As flatmap
    val getItem = httpclient.get(ZioHNApi.getItemURL(11498534)).flatMap {
      unParsed => ZioHNApi.parseItemResponse(unParsed)
    }

    val result: Exit[Throwable, HNItem] = runtime.unsafeRunSync(getItem)

    result.fold(
      _ => fail,
      item => assert(item.by == "justinhj")
    )

  }

  "Top stories" should "parse correctly" in {

    val runtime = new TestRuntime {}

    // As flatmap
    val getTopStories = httpclient.get(getTopItemsURL).flatMap {
      unParsed => ZioHNApi.parseTopItemsResponse(unParsed)
    }

    /*
    // As for comprehension
    val getTopStories2 = for(
      unParsed <- httpclient.get(getTopItemsURL);
      items <- ZioHNApi.parseTopItemsResponse(unParsed)
    ) yield items

    // As desugared for comprehension
    val getTopStories3 = httpclient.get(getTopItemsURL).flatMap(unParsed =>
      ZioHNApi.parseTopItemsResponse(unParsed).flatMap(items =>
        ZioHNApi.parseTopItemsResponse(unParsed).flatMap(items2 =>
          ZioHNApi.parseTopItemsResponse(unParsed).map(items3 => items ++ items2 ++ items3))))
    */

    val result: Exit[Throwable, HNItemIDList] = runtime.unsafeRunSync(getTopStories)

    result.fold(
      _ => fail,
      items => assert(items.size == 4)
    )
  }



}
