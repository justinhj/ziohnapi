package org.justinhj.httpclient

import scalaj.http.{BaseHttp, HttpConstants, HttpOptions}
import scalaz.zio.ZIO
import scalaz.zio.blocking._

import scala.util.{Failure, Success, Try}

trait HttpClient {
  val httpClient : HttpClient.Service[Any with HttpClient with Blocking]

}
object HttpClient {

  trait Service[R <: HttpClient with Blocking] {
    def get(url: String): ZIO[R, Throwable, String]
  }

  // Implementation using ScalaJ

  trait HttpClientLive extends HttpClient {

    def customOptions: Seq[HttpOptions.HttpOption] = Seq(
      HttpOptions.connTimeout(5000),
      HttpOptions.readTimeout(5000),
      HttpOptions.followRedirects(true)
    )

    object customHttp extends BaseHttp(
      proxyConfig = None,
      options = customOptions,
      charset = HttpConstants.utf8,
      sendBufferSize = 4096,
      userAgent = "justinhj/httpclient/1.0",
      compress = true
    )

    val httpClient: Service[Any with HttpClient with Blocking] = new Service[Any with HttpClient with Blocking] {

      def requestSync(url: String) : String = {

        // TODO pass logging environment
        //println(s"url get on thread ${Thread.currentThread().getName}")

        Try(customHttp(url).asString) match {

          case Success(response) =>

            if (response.code == 200) {
              Try{
                //println(s"parsing ${response.body.take(40)}")

                response.body
              } match {
                case Success(good) if good == null =>
                  //println("got empty")
                  throw new Exception("Not found")
                case Success(good) =>
                  //println(s"got url on thread ${Thread.currentThread().getName}")
                  good
                case Failure(e) =>
                  //println(s"got parse error ${response.body}")
                  throw new Exception(s"Failed to read ${e.getMessage}")
              }
            }
            else {
              //println("got no response")
              throw new Exception(s"Failed to retrieve $url code: ${response.code}")
            }
          case Failure(err) =>
            throw new Exception(s"Failed to retrieve $url error: ${err.getMessage}")
        }
      }

      final def get(url: String) : ZIO[Blocking, Throwable, String] = {
        blocking(ZIO.effect(requestSync(url)))
      }

    }

  }

  object Live extends HttpClient.HttpClientLive

}
