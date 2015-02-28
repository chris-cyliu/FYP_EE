package common

import org.joda.time.{DateTimeZone, DateTime}
import play.api.db.DB
import play.api.libs.json._
import play.api.Play.current

/**
 * Created by fafa on 25/2/15.
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
      stmt.setDate(1,new java.sql.Date(record.date.getMillis))
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
}