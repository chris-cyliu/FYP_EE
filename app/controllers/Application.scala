package controllers

import actor.WebSocketActor
import play.api.libs.json.JsValue
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
}