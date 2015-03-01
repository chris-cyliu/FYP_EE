package actor

import java.net.InetSocketAddress
import actor.PushAggData
import akka.actor._
import akka.io.{Udp, IO, Tcp}
import common.Constant
import model.{ArdiunoMessage, Record}
import org.joda.time.{DateTimeZone, DateTime}
import play.api.libs.json._

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

  val agg_actor = context.actorSelection("/user/"+Constant.actor_name_agg)

  override def receive: Actor.Receive = {
    case NewBrowser =>
      ws_list += sender()
      agg_actor ! PushAggData
      log.info("WS Router receive A Browser join , number of listening browser : "+ws_list.size)
    case m:NewRecord =>
      //notice all ws
      ws_list.map(_ ! m)
    case m:PushAggData =>
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
    case PushAggData(json) =>
      out ! Json.obj("event"->"push_agg_data","data"->json)
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

  val taskActor = context.actorSelection("/user/" + Constant.actor_name_db) ::
    context.actorSelection("/user/" + Constant.actor_name_wsr) ::
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
          taskActor.map({
            _ ! NewRecord(Record(m.data))
          })
      }
    case Udp.Unbind => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
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

  val taskActor = context.actorSelection("/user/" + Constant.actor_name_db) ::
    context.actorSelection("/user/" + Constant.actor_name_wsr) ::
    Nil

  override def receive: Actor.Receive = {
    case Received(data) =>
      val m = ArdiunoMessage(data.decodeString("US-ASCII"))
      m.event match {
        case ArdiunoMessage.event_add_record =>
          taskActor.map({
            _ ! NewRecord(Record(m.data))
          })
      }

  }
}

object DBAggregatorActor{
  def props() = Props(new DBAggregatorActor)
}
/**
 * Actor to cache aggreator
 *
 */
class DBAggregatorActor extends Actor with ActorLogging{

  val wsr = context.actorSelection("/user/" + Constant.actor_name_wsr)

  object AggregatorData {

    var data:JsObject = null
    getData
    def getData = {
      data =
        Json.obj(
          "average"-> toJson(Record.getAverage(DateTime.now(DateTimeZone.UTC).minusHours(24) , DateTime.now(DateTimeZone.UTC))),
          "max" -> toJson(Record.getMax(DateTime.now(DateTimeZone.UTC).minusHours(24), DateTime.now(DateTimeZone.UTC)))
        )
    }

    def toJson(data:Seq[(Int,Double)]):JsValue ={
      var ret = Json.obj()
      data.foreach({a =>
        ret = ret + (a._1.toString,JsNumber(a._2))
      })
      Json.toJson(ret)
    }
  }
  override def receive : Actor.Receive ={
    case UpdateAggData =>
      AggregatorData.getData
      self ! PushAggData
    case PushAggData =>
      log.debug("Start push aggregator data")
      wsr ! PushAggData(AggregatorData.data)
  }
}