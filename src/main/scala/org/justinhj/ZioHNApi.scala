package org.justinhj

import org.justinhj.httpclient.HttpClient.ScalaJHttpClient
import scalaz.zio.{App, ZIO}
import scalaz.zio.console.{putStrLn, _}

// Accessing the Hacker News API via Scalaz and the ZIO library
// an example of purely functional code that handles effects and
// can be sensibly tested

// Stuck on how to extend the environment with my own services


object ZioHNApi extends App {

  def run(args: List[String]) =
    sample(args).fold(_ => 1, _ => 0)

  def sample(args: List[String]) : ZIO[Console with ScalaJHttpClient, Throwable, Unit] = {
    putStrLn(s"There are ${args.length} args").flatMap{_ => httpclient.get("http://www.microsoft.com")}
  }

}
