package examples

import org.justinhj.hnapi.HNApi._
import zio.console._
import zio.{Task, ZIO}

object ShowStoryComments {

  def main(args: Array[String]): Unit = {

    val runtime = new LiveRuntime {}

    val getItemId = Task.effect {
      args(0).toInt
    }

    val program = (for (
      itemId <- getItemId;
      itemsAndKids <- getItemAndKids(itemId);
      _ <- showComments(itemId, itemsAndKids)
    ) yield ()).foldM(
      err =>
        putStrLn(s"Program threw exception. $err"),
      succ => ZIO.succeed(())
    )

    runtime.unsafeRunSync(program)
  }

}