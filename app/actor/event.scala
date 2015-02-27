package actor

import common.Record

/**
 * Event class
 */
case class NewRecord(record:Record)

case class NewBrowser()

case class QuitBrowser()