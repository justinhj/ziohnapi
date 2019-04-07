package examples

import org.justinhj.httpclient.HttpClient
import scalaz.zio.Runtime
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalaz.zio.internal.{Platform, PlatformLive}
import scalaz.zio.random.Random
import scalaz.zio.system.System


trait LiveRuntime extends Runtime[Clock with Console with System with Random with Blocking with HttpClient] {
  type Environment = Clock with Console with System with Random with Blocking with HttpClient

  val Platform: Platform       = PlatformLive.Default
  val Environment: Environment = new Clock.Live with Console.Live with System.Live with Random.Live with Blocking.Live
    with HttpClient.HttpClientLive
}
