package actor

import java.net.InetSocketAddress
import akka.actor._
import akka.io.{Udp, IO, Tcp}
import common.{ArdiunoMessage, Constant, Record}
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
class WebSocketRouterActor extends Actor with ActorLogging{

  val ws_list = collection.mutable.Set[ActorRef]()

  override def receive: Actor.Receive = {
    case NewBrowser =>
      ws_list += sender()
      log.info("WS Router receive A Browser join , number of listening browser : "+ws_list.size)
    case m:NewRecord =>
      //notice all ws
      ws_list.map(_ ! m)
    case QuitBrowser =>
      ws_list -= sender()
      log.info("A Browser quit, number of remain browser : "+ws_list.size)

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

  val router = context.actorSelection("/user/"+Constant.actor_name_wsr)

  router ! NewBrowser

  override def receive: Actor.Receive = {

    //push back record to the client
    case NewRecord(record) =>
      out ! Json.obj("event"->"push_data","data"->Json.toJson(record))
    case _ =>
  }

  override def postStop(): Unit = {
    router ! QuitBrowser
  }
}

/**
 * UDP Message Handler
 */
object UDPMessageActor{
  def props = Props(new UDPMessageActor)
}

class UDPMessageActor extends Actor {

  import context.system
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("0.0.0.0", 5858))

  val taskActor = context.actorSelection("/user/"+Constant.actor_name_db)::
    context.actorSelection("/user/"+Constant.actor_name_wsr)::
    Nil

  override def receive = {
    case Udp.Bound(local) =>
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      val m = ArdiunoMessage(data.decodeString("US-ASCII"))
      m.event match {
        case ArdiunoMessage.event_add_record =>
          taskActor.map({ _ ! NewRecord(Record(m.data)) })
      }
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
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
        log.info(s"A logger connected")
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