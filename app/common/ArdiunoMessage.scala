package common

/**
 * Created by fafa on 26/2/15.
 */
case class ArdiunoMessage(event:String,data:String)

object ArdiunoMessage{

  val event_add_record = "addRecord"

  def apply(message:String):ArdiunoMessage = {
    val fields = message.split(":")
    if (fields.size != 2) {
      throw new Exception(s"Unknow tcp message: $message")
    }
    ArdiunoMessage(fields(0), fields(1))
  }
}
