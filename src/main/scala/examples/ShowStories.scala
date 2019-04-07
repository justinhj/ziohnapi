package examples


import org.justinhj.hnapi.HNApi._
import org.justinhj.httpclient
import org.justinhj.httpclient.HttpClient
import scalaz.zio.{Runtime, ZIO}
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console._
import scalaz.zio.internal.{Platform, PlatformLive}
import scalaz.zio.random.Random
import scalaz.zio.system.System


trait LiveRuntime extends Runtime[Clock with Console with System with Random with Blocking with HttpClient] {
  type Environment = Clock with Console with System with Random with Blocking with HttpClient

  val Platform: Platform       = PlatformLive.Default
  val Environment: Environment = new Clock.Live with Console.Live with System.Live with Random.Live with Blocking.Live
    with HttpClient.HttpClientLive
}

object ShowStories {

  def main(args: Array[String]): Unit = {

    val runtime = new LiveRuntime {}

    val program = (for (
      s <- httpclient.get(getTopItemsURL);
      items <- parseTopItemsResponse(s);
      _ <- showPagesLoop(items)
    ) yield ()).foldM(
      err =>
        putStrLn(s"Program threw exception. ${err.getMessage}"),
      succ => ZIO.succeed(())
    )

    runtime.unsafeRunSync(program)
  }

}
