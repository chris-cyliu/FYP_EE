package actor

import model.Record
import play.api.libs.json.JsObject

/**
 * Event class
 */
case class NewRecord(record:Record)

case class NewBrowser()

case class QuitBrowser()

case class PushAggData(data:JsObject)

case class UpdateAggData()