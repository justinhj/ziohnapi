package org.justinhj

import scalaz.zio.ZIO

package object httpclient {
  final def get(url: String): ZIO[HttpClient, Throwable, String] =
    ZIO.accessM(_.httpClient get url)

}
