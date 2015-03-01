package controllers

import actor.WebSocketActor
import model.Record
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Json, JsValue}
import play.api.mvc._
import play.api.Play.current

object Application extends Controller {

  def index = Action {
    Ok(views.html.main())
  }

  //build
  def ws = WebSocket.acceptWithActor[JsValue,JsValue]{
    request => out => WebSocketActor.props(out)
  }

  def history = Action{
    Ok(views.html.history())
  }

  /**
   * http://127.0.0.1:8181/query?from=1425168000&to=1425230033&value_type=1
   * @return
   */
  def query = Action{
    implicit request =>
      val from = new DateTime(request.getQueryString("from").get.toLong,DateTimeZone.UTC)
      val to = new DateTime(request.getQueryString("to").get.toLong,DateTimeZone.UTC)
      val v_type = request.getQueryString("value_type").get.toInt

      val data = Record.query(from,to,v_type)
      Ok(Json.obj("data"->data))
  }
}