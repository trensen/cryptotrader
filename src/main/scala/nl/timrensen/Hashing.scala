package nl.timrensen

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
  * Created by trensen on 15/01/2016.
  */
object Hashing {
    def HMAC_SHA512(msg: String, key: String): String = {
      val secret = new SecretKeySpec(key.getBytes, "HmacSHA512")
      val mac = Mac.getInstance("HmacSHA512")
      mac.init(secret)
      val result: Array[Byte] = mac.doFinal(msg.getBytes)
      result.map("%02x" format _).mkString
    }
}
