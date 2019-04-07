package org.justinhj

import org.justinhj.hnapi.HNApi
import org.justinhj.hnapi.HNApi.HNItem
import org.justinhj.httpclient.HttpClient
import org.justinhj.httpclient.HttpClient.Service
import org.scalatest.FlatSpec
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalaz.zio.internal.{Platform, PlatformLive}
import scalaz.zio.random.Random
import scalaz.zio.system.System
import scalaz.zio.{Exit, IO, Runtime, Task, ZIO}


// Test suite for ZIO HackerNews API
class HNApiTest extends FlatSpec {

  // The test http runtime
  trait HttpClientTest extends HttpClient {

    val sampleTopStories = "[19536375,19536173,19535059,19535564]"
    val sampleItem = """{"by":"justinhj","id":11498534,"parent":11498393,"text":"I&#x27;m an emacs user but I often use a GUI IDE when it makes sense. I find I&#x27;d rather use JetBrains products (PHPStorm, PyCharm and IntelliJ) even though the actual text editing part is nowhere near as powerful and efficient as my Emacs setup.  Sometimes if I need to do some repetitive or monumental editing task that I could script in emacs or use the macro system, I&#x27;ll switch over for a moment. The benefits of being in an environment with a debugger and many other tools as well as project management and auto-completion that is consistent across the languages I use outweigh the cons of leaving emacs. \nI&#x27;m considering switching to Visual Studio just for a React Native project I&#x27;m working on because it seems to handle Javascript&#x2F;JMX much better than anything else I&#x27;ve tried.","time":1460653869,"type":"comment"}"""

    val httpClient: Service[Any with HttpClient with Blocking] = new Service[Any with HttpClient with Blocking] {

      def requestSync(url: String) : String = {
        if(url == HNApi.getTopItemsURL) sampleTopStories
        else if(url == HNApi.getItemURL(11498534)) sampleItem
        else throw new Exception(s"$url not found in http mock client")
      }

      final def get(url: String) : Task[String] = {
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
    val getItem = httpclient.get(HNApi.getItemURL(11498534)).flatMap {
      unParsed => HNApi.parseItemResponse(unParsed)
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
    val getTopStories = httpclient.get(HNApi.getTopItemsURL).flatMap {
      unParsed => HNApi.parseTopItemsResponse(unParsed)
    }

    val result: Exit[Throwable, HNApi.HNItemIDList] = runtime.unsafeRunSync(getTopStories)

    result.fold(
      _ => fail,
      items => assert(items.size == 4)
    )
  }



}
