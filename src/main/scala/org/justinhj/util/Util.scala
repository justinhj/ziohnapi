package org.justinhj.util

import java.net.URL
import java.util.Date

import org.ocpsoft.prettytime.PrettyTime

import scala.util.{Failure, Success, Try}

object Util {
  // Let's display the time like "2 minutes ago" using the PrettyTime library
  // ts is epoch time in seconds
  def timestampToPretty(ts: Int) : String = {

    val epochTimeMS = ts * 1000L

    val p = new PrettyTime()
    p.format(new Date(epochTimeMS))
  }

  // We will display just the hostname of the URL
  // this returns close to what we want but not exactly...
  def getHostName(url: String) : String = {
    if(url.isEmpty) ""
    else {
      Try(new URL(url)) match {
        case Success(u) =>
          "(" + u.getHost + ")"
        case Failure(e) =>
          ""
      }
    }
  }
}
