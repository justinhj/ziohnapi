package examples

import org.justinhj.hnapi.HNApi._
import org.justinhj.httpclient
import zio.ZIO
import zio.console._

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
