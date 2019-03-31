package org.justinhj

import org.justinhj.ZioHNApi.getTopItemsURL
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

    val httpClient: Service[Any] = new Service[Any] {

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

  "Top stories" should "parse correctly in" in {

    val runtime = new TestRuntime {}

    val getTopStories = httpclient.get(getTopItemsURL)

    val result: Exit[Throwable, String] = runtime.unsafeRunSync(getTopStories)

    result.fold(
      _ => fail,
      success => assert(success == HttpClientTest.sampleTopStories)
    )

    assert(1+1 == 2)

  }



}
