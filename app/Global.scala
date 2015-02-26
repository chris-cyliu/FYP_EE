import actor.{DatabaseActor, WebSocketRouterActor, TCPServerActor}
import common.Constant
import play.api.libs.concurrent.Akka
import play.api.{Application, GlobalSettings}
import play.api.Play.current

object Global extends GlobalSettings {

  override def onStart(app:Application){

    //Initialize actors
    Akka.system.actorOf(TCPServerActor.props)
    Akka.system.actorOf(WebSocketRouterActor.props, Constant.actor_name_wsr)
    Akka.system.actorOf(DatabaseActor.props,Constant.actor_name_db)
  }
}