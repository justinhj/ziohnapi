package org.justinhj

import zio.ZIO
import zio.blocking.Blocking

package object httpclient extends HttpClient.Service[HttpClient with Blocking] {
  final def get(url: String): ZIO[Any with HttpClient with Blocking, Throwable, String] =
    ZIO.accessM(_.httpClient.get(url))
}
