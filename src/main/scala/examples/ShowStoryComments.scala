package examples

import org.justinhj.hnapi.HNApi._
import org.justinhj.httpclient
import scalaz.zio.console._
import scalaz.zio.{Task, ZIO}

object ShowStoryComments {

  def main(args: Array[String]): Unit = {

    val runtime = new LiveRuntime {}

    val getItemId = Task.effect {
      args(0).toInt
    }

    val program = (for (
      itemId <- getItemId;
      itemResponse <- httpclient.get(getItemURL(itemId));
      item <- parseItemResponse(itemResponse);
      commentResponse <- httpclient.get(getItemURL(item.kids(0)));
      _ <- putStrLn(s"How many kids ${item.kids.size}");
      commentItem <- parseItemResponse(commentResponse);
      _ <- showComment(commentItem)
    ) yield ()).foldM(
      err =>
        putStrLn(s"Program threw exception. ${err.getMessage}"),
      succ => ZIO.succeed(())
    )

    runtime.unsafeRunSync(program)
  }

}