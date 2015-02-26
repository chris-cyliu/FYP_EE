package common

/**
 * Created by fafa on 26/2/15.
 */
case class TCPMessage(event:String,data:String)

object TCPMessage{

  val event_add_record = "addRecord"

  def apply(message:String):TCPMessage = {
    val fields = message.split(":")
    if (fields.size != 2) {
      throw new Exception(s"Unknow tcp message: $message")
    }
    TCPMessage(fields(0), fields(1))
  }
}
