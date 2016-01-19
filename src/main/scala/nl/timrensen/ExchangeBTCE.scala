package nl.timrensen

import java.io.{FileOutputStream, FileInputStream}

import org.json4s.{DefaultFormats, JValue}
import rx.lang.scala.Observable

import java.util.Properties


object ExchangeBTCE {
  private val prop = new Properties
  prop.load(new FileInputStream("config/data.cfg"))

  val key: String = prop.getProperty("key")
  val secret: String = prop.getProperty("secret")
  var nonce: Int = Integer.parseInt(prop.getProperty("nonce"))

  implicit val formats = DefaultFormats

  private def doRequest[T](params: Map[String, String], func: JValue => T)(implicit m: Manifest[T]): Observable[T] = {
    val paramsWithNonce = params + ("nonce" -> getNonce.toString)
    val postString = paramsWithNonce.map { case (k,v) => k + "=" + v }.mkString("&")

    val headers =  Map(
      "Key"  -> key,
      "Sign" -> Hashing.HMAC_SHA512(postString, secret))

    val response: Observable[JValue] = Requester.doPost("https://btc-e.com/tapi", paramsWithNonce, headers)

    response.flatMap(f => {
      if((f \ "success").extract[Int] == 1) {
          Observable.just(func(f \ "return"))
      }
      else
        Observable.error(new Exception((f \ "error").extract[String]))
    })
  }

  private def doRequest[T](params: Map[String, String])(implicit m: Manifest[T]): Observable[T] = doRequest(params, _.extract[T])






  def getInfo: Observable[Info] = {
    doRequest[Info](Map("method" -> "getInfo"))
  }

  def placeOrder(pair: String, orderType: String, rate: Double, amount: Double): Observable[NewOrder] = {
    doRequest[NewOrder](Map(
      "method" -> "Trade",
      "pair" -> pair,
      "type" -> orderType,
      "rate" -> rate.toString,
      "amount" -> amount.toString))
  }

  def getActiveOrders: Observable[List[ActiveOrder]] = {
    doRequest(Map("method" -> "ActiveOrders"), extractActiveOrders)
      .onErrorResumeNext(ex => {
        if("no orders".equals(ex.getMessage))
          Observable.just(List.empty)
        else
          Observable.error(ex)
      })
      .map(_.toList)
  }

  def getOrderInfo(orderId: Int): Observable[ActiveOrder] = {
    doRequest(Map("method" -> "OrderInfo", "order_id" -> orderId.toString), extractActiveOrders)
        .map(_.head)
  }

  def cancelOrder(orderId: Int): Observable[CanceledOrder] = {
    doRequest[CanceledOrder](Map("method" -> "CancelOrder", "order_id" -> orderId.toString))
  }

  def getTradeHistory: Observable[List[Trade]] = {
    doRequest(Map("method" -> "TradeHistory"), extractTrades)
      .onErrorResumeNext(ex => {
        if("no trades".equals(ex.getMessage))
          Observable.just(List.empty)
        else
          Observable.error(ex)
      })
      .map(_.toList)
  }

  def getTransHistory: Observable[List[Transaction]] = {
    doRequest(Map("method" -> "TransHistory"), extractTransactions)
      .map(_.toList)
  }



  // TODO: combine
  private def extractActiveOrders(jval: JValue): Iterable[ActiveOrder] = {
    jval.extract[Map[String, ActiveOrder]].map { case (k, v) => v.copy(id = Integer.parseInt(k))}
  }
  private def extractTransactions(jval: JValue): Iterable[Transaction] = {
    jval.extract[Map[String, Transaction]].map { case (k, v) => v.copy(id = Integer.parseInt(k))}
  }
  private def extractTrades(jval: JValue): Iterable[Trade] = {
    jval.extract[Map[String, Trade]].map { case (k, v) => v.copy(id = Integer.parseInt(k))}
  }


  protected def getNonce: Int = {
    this.nonce += 1

    prop.setProperty("nonce", nonce.toString)
    prop.store(new FileOutputStream("config/data.cfg"), "Data")

    nonce
  }
}


trait Result
case class Info(
                       funds: Balance,
                       rights: Rights,
                       transaction_count: Int,
                       open_orders: Int,
                       server_time: Int
                       ) extends Result

case class Balance(     usd: Double,
                        btc: Double,
                        ltc: Double,
                        nmc: Double,
                        rur: Double,
                        eur: Double,
                        nvc: Double,
                        trc: Double,
                        ppc: Double,
                        ftc: Double,
                        xpm: Double,
                        cnh: Double,
                        gbp: Double
                      ) extends Result

case class Rights( info: Int,
                        trade: Int,
                        withdraw: Int
                      ) extends Result

case class Transaction( id: Int = -1,
                        `type`: Int,
                        amount: Double,
                        currency: String,
                        desc: String,
                        status: Int,
                        timestamp: Int) extends Result


case class ActiveOrder(id: Int = -1,
                 pair: String,
                 `type`: String,
                 start_amount: Option[String],
                 amount: Double,
                 rate: Double,
                 timestamp_created: Int,
                 status: Int) extends Result


case class CanceledOrder(order_id: Int,
                         funds: Balance) extends Result


case class NewOrder(received: Double,
                    remains: Double,
                    order_id: Int,
                    funds: Balance) extends Result

case class Trade( id: Int = -1,
                  pair: String,
                 `type`: String,
                  amount: Double,
                  rate: Double,
                  order_id:Int,
                  is_your_order: Boolean,
                  timestamp: Int) extends Result