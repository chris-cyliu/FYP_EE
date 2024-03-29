import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

import actor._
import common.Constant
import play.api.libs.concurrent.Akka
import play.api.{Application, GlobalSettings}
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

object Global extends GlobalSettings {

  override def onStart(app: Application) {

    //Initialize actors
    Akka.system.actorOf(UDPMessageActor.props)
    Akka.system.actorOf(WebSocketRouterActor.props, Constant.actor_name_wsr)
    Akka.system.actorOf(DatabaseActor.props, Constant.actor_name_db)
    val aggActor = Akka.system.actorOf(DBAggregatorActor.props, Constant.actor_name_agg)

    //Scheduler
    Akka.system.scheduler.schedule(Duration.create(Constant.push_interval_second, TimeUnit.SECONDS),
      Duration.create(Constant.push_interval_second, TimeUnit.SECONDS),
      aggActor, UpdateAggData)

    Akka.system.scheduler.schedule(Duration.create(Constant.push_interval_second, TimeUnit.SECONDS),
      Duration.create(Constant.push_interval_second, TimeUnit.SECONDS),
      aggActor, PushAggData)
  }
}
