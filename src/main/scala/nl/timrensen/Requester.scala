package nl.timrensen

import dispatch._, Defaults._
import org.json4s
import rx.lang.scala.Observable

/**
  * Created by trensen on 16/01/2016.
  */
object Requester {
  def doPost(urlString: String, params: Map[String, String], headers: Map[String, String]): Observable[json4s.JValue] = {
    val request = url(urlString) <:<headers <<params
    Observable.from(Http(request OK as.json4s.Json))
  }
}
