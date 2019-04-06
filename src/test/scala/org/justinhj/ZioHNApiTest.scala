package org.justinhj

import org.justinhj.ZioHNApi.{HNItemIDList, getTopItemsURL}
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

    val httpClient: Service[Any with HttpClient with Blocking] = new Service[Any with HttpClient with Blocking] {

      def requestSync(url: String) : String = {
        if(url == ZioHNApi.getTopItemsURL) sampleTopStories
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
