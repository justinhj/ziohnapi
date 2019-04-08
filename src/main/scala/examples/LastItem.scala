package examples

import org.justinhj.hnapi.HNApi._
import org.justinhj.httpclient
import scalaz.zio.Schedule
import scalaz.zio.duration._

object LastItem {

  def main(args: Array[String]): Unit = {

    val runtime = new LiveRuntime {}

    val showLastItem = for (
      maxItemResponse <- httpclient.get(getMaxItemURL);
      maxItem <- parseMaxItemResponse(maxItemResponse);
      itemResponse <- httpclient.get(getItemURL(maxItem));
      item <- parseItemResponse(itemResponse);
      _ <- showComment(item)
    ) yield ()

    val program = showLastItem.repeat(Schedule.spaced(10.seconds))

    runtime.unsafeRunSync(program)
  }

}
