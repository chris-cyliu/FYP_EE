import java.io.PrintStream
import java.net.{Socket, InetAddress}

/**
 * Created by fafa on 26/2/15.
 */
object TcpTest {

  def main(args:Array[String]): Unit ={
    val socket = new Socket("localhost",5858)
    val out = new PrintStream(socket.getOutputStream)

    out.println("addRecord:1;1;1.0")
    out.flush()
    out.close()
    println("succesfully ")
  }

}
