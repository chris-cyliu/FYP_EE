package actor

import java.net.InetSocketAddress
import akka.actor._
import akka.io.{IO, Tcp}
import common.{TCPMessage, Constant, Record}
import play.api.libs.json.Json

/**
 * Actor for database action
 */

object DatabaseActor {
  def props = Props(new DatabaseActor)
}
class DatabaseActor extends Actor with ActorLogging{

  override def receive: Receive = {
    case NewRecord(record) =>
      try {
        Record.insertToDb(record)
      }catch{
        case e:Exception =>
          log.error(e,"Fail to process the event")
      }

  }
}

object WebSocketRouterActor{
  def props = Props(new WebSocketRouterActor())
}
class WebSocketRouterActor extends Actor {

  val ws_list = collection.mutable.Buffer[ActorRef]()

  override def receive: Actor.Receive = {
    case NewBrowser =>
      ws_list += sender()
    case m:NewRecord =>
      //notice all ws
      ws_list.map(_ ! m)
      //TODO: disconnect ws

  }
}
object WebSocketActor{
  def props(out:ActorRef) = Props(new WebSocketActor(out))
}
/**
 * Actor for handle websocket action
 * @param out
 */
class WebSocketActor(out:ActorRef) extends Actor with ActorLogging{

  context.actorSelection("/user/"+Constant.actor_name_wsr) ! NewBrowser

  log.info("WS openned preparing")

  override def receive: Actor.Receive = {

    //push back record to the client
    case NewRecord(record) =>
      out ! Json.obj("event"->"push_data","data"->Json.toJson(record))
    case _ =>
  }
}

object TCPServerActor{
  def props = Props(new TCPServerActor())
}
/**
 * Handle TCP connection request
 */
class TCPServerActor extends Actor with ActorLogging{
    import Tcp._
    import context.system

    IO(Tcp) ! Bind(self, new InetSocketAddress("0.0.0.0", Constant.tcp_port))

    override def receive: Actor.Receive = {
      case b @ Bound(localAddress) =>
        log.info(s"tcp: $localAddress try to connect")

      case CommandFailed(_: Bind) => context stop self

      case c @ Connected(remote, local) =>
        val handler = context.actorOf(TCPMessageActor.props(sender()))
        val connection = sender()
        connection ! Register(handler)
    }

}


/**
 * TCP Message Handler
 */
object TCPMessageActor{
  def props(connection:ActorRef) = Props(new TCPMessageActor(connection))
}

class TCPMessageActor(connection:ActorRef) extends Actor {

  import Tcp._

  context watch connection

  val taskActor = context.actorSelection("/user/"+Constant.actor_name_db)::
    context.actorSelection("/user/"+Constant.actor_name_wsr)::
    Nil

  override def receive: Actor.Receive = {
    case Received(data) =>
      val m = TCPMessage(data.decodeString("US-ASCII"))
      m.event match {
        case TCPMessage.event_add_record =>
          taskActor.map({ _ ! NewRecord(Record(m.data)) })
      }

  }
}