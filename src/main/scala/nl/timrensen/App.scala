package nl.timrensen

import dispatch._, Defaults._

/**
 * @author ${user.name}
 */
object App {
  def main(args : Array[String]) {
    ExchangeBTCE.getInfo.subscribe(
      next => println("RESULT: " + next),
      ex => {
        println("OOPS: " + ex.getMessage)
        System.exit(0)
      },
      () => System.exit(0))
  }
}
