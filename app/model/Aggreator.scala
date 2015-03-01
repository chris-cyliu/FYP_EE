package model

/**
 * case class fro aggreator
 */
trait Aggregator {
  val op_name : String
}

case class Average() extends Aggregator{
  val op_name = "avg"
}

case class Maximum() extends Aggregator{
  val op_name = "max"
}