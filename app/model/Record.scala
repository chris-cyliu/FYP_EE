package model

import java.sql.{Timestamp, Time}
import java.text.SimpleDateFormat
import java.util.Date

import org.joda.time.{DateTime, DateTimeZone}
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json._

/**
 * Bean for each sensor record
 * @param device_id
 * @param v_type
 * @param value
 * @param date
 */
case class Record(device_id:Int, v_type:Int, value:Double,date:DateTime = DateTime.now(DateTimeZone.UTC))

/**
 * Encode string format
 * {device_id};{v_type};{value}
 */
object Record{

  val ds = DB.getDataSource()

  val table_name = "record"

  createIfNotExist

  def apply(encode_str:String):Record = {
    val fields = encode_str.split(";")
    if(fields.size != 3 ){
      throw new Exception("Message fields not equal to 3")
    }
    Record(fields(0).toInt, fields(1).toInt, fields(2).toDouble)
  }

  /** json serialization
    *
    */
  implicit val recordWrite  = new Writes[Record]{
   def writes(record:Record) = Json.obj(
    "date" -> record.date.getMillis(),
    "device_id" -> record.device_id,
    "v_type" ->record.v_type,
    "value" -> record.value
   )
  }

  /**
   * Try to insert data to the database
   * @param record
   */
  def insertToDb(record:Record): Unit = {
    //add to data base
    val conn = ds.getConnection()
    val stmt = conn.prepareStatement(s"INSERT INTO $table_name VALUES (?,?,?,?)")
    try{
      stmt.setTimestamp(1,new java.sql.Timestamp(record.date.getMillis))
      stmt.setInt(2,record.device_id)
      stmt.setInt(3,record.v_type)
      stmt.setDouble(4,record.value)
      stmt.executeUpdate()
      conn.commit()
    }catch{
      case e:Exception =>
        throw e
    }finally{
      stmt.close()
      conn.close()
    }
  }

  def createIfNotExist() = {
    val conn = ds.getConnection()
    val stmt = conn.createStatement()
    try{
      stmt.execute(s"CREATE TABLE if not exists $table_name (date timestamp , device_id INTEGER , v_type INTEGER,value DOUBLE , primary key(date , device_id , v_type))")
      conn.commit()
    }catch {
      case e: Exception =>
        throw e
    }finally {
      stmt.close
      conn.close
    }
  }

  def getAggregator(from:DateTime , to:DateTime , op:Aggregator):Seq[(Int,Double)] = {
    val conn = ds.getConnection()
    val op_name = op.op_name
    val stmt = conn.prepareStatement(s"SELECT v_type, $op_name(value) as op_value FROM $table_name WHERE date >= ? AND date <= ? GROUP BY v_type")
    try{
      stmt.setTimestamp(1 , new Timestamp(from.getMillis))
      stmt.setTimestamp(2 , new Timestamp(to.getMillis))
      val ret_set = stmt.executeQuery()
      var ret = collection.mutable.Buffer[(Int,Double)]()
      while(ret_set.next){
        ret += Tuple2(ret_set.getInt("v_type"), ret_set.getDouble("op_value"))
      }
      ret
    }catch {
      case e: Exception =>
        throw e
    }finally {
      stmt.close
      conn.close
    }
  }

  def getAverage(from:DateTime , to:DateTime):Seq[(Int,Double)] = {
    getAggregator(from,to,Average())
  }

  def getMax(from:DateTime , to:DateTime):Seq[(Int,Double)] = {
    getAggregator(from,to,Maximum())
  }

  def query(from:DateTime , to:DateTime, valueType:Int) = {
    val conn = ds.getConnection();
    val stmt = conn.prepareStatement(s"SELECT date , value from $table_name WHERE date <= ? AND date >= ? AND v_type = ?")
    try{
      stmt.setTimestamp(1 , new Timestamp(to.getMillis))
      stmt.setTimestamp(2 , new Timestamp(from.getMillis))
      stmt.setInt(3,valueType)
      val date_format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
      var ret = JsArray()
      val ret_set = stmt.executeQuery()
      while(ret_set.next()){
        var temp = JsArray()
        temp = temp :+ JsString(date_format.format(new Date(ret_set.getTimestamp(1).getTime)))
        temp = temp :+ JsNumber(ret_set.getDouble(2))
        ret = ret :+ temp
      }
      ret
    }
  }
}

