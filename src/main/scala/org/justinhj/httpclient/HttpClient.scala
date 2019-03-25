package org.justinhj.httpclient

import scalaz.zio.{ZIO, IO}

trait HttpClient {
  val httpClient : HttpClient.Service[Any]

}
object HttpClient {

  trait Service[R] {
    def get(url: String): IO[Throwable, String]
  }

  // Implementation using ScalaJ

  trait ScalaJHttpClient extends HttpClient {

    val httpClient: Service[Any] = new Service[Any] {

      final def get(url: String) : IO[Throwable, String] = {

        ZIO.succeed(url)

      }

    }

  }

}

object ScalaJHttpClient extends HttpClient.ScalaJHttpClient
