package io.udash.rpc.serialization.jawn

import jawn.SimpleFacade

object JawnFacade extends SimpleFacade[JValue] {
  override def jnull() = JNull

  override def jtrue() = JBoolean(true)

  override def jfalse() = JBoolean(false)

  override def jnum(s: String): JNum =
    try { JInt(s.toInt) }
    catch {
      case ex: NumberFormatException =>
        JDouble(s.toDouble)
    }

  override def jint(s: String) = JInt(s.toInt)

  override def jstring(s: String) = JString(s)

  override def jarray(vs: List[JValue]) = JList(vs)

  override def jobject(vs: Map[String, JValue]): JValue = JObject(vs)
}
