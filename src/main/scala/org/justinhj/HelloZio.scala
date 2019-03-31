package org.justinhj

import java.io.IOException

import scalaz.zio.{App, UIO, ZIO}
import scalaz.zio.console.{putStrLn, _}

object HelloZio extends App {

  def run(args: List[String]) =
    myAppLogic.fold(_ => 1, _ => 0)

  def validName(s: String) : UIO[Boolean] = {
    val legalChars = s.forall{p => p.isLetter || p == '-' || p == ' '}
    val beginsLetter = s.headOption.map(_.isLetter).getOrElse(false)

    ZIO.succeed(legalChars && beginsLetter)
  }

  val myAppLogic: ZIO[Console, IOException, Unit] =
    for {
      _ <- putStrLn("Hello! What is your name?")
      input <- getStrLn
      valid <- validName(input)
      _ <- if (valid)
              putStrLn(s"Hello, ${input}, welcome to ZIO!")
            else
              putStrLn(s"Hello, I'm not sure ${input} is a valid name.")
    } yield ()
}