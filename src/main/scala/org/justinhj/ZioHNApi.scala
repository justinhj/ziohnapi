package org.justinhj

import org.justinhj.httpclient.HttpClient.Live

import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.{App, ZIO}
import scalaz.zio.console.{putStrLn, _}
import scalaz.zio.random.Random
import scalaz.zio.system.System

// Accessing the Hacker News API via Scalaz and the ZIO library
// an example of purely functional code that handles effects and
// can be sensibly tested

trait HNApiApp extends App with HttpClient {
  override val Environment: Environment = new Clock.Live with Console.Live with System.Live with Random.Live
      with Blocking.Live with HttpClient.Live

}

object HNApiApp extends HNApiApp {

  def run(args: List[String]) =
    sample(args).fold(_ => 1, _ => 0)

  def sample(args: List[String]) : ZIO[HNApiApp, Throwable, Unit] = {
    putStrLn(s"There are ${args.length} args").flatMap{_ => httpclient.get("http://www.microsoft.com")}
  }

}
