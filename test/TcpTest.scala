import java.io.PrintStream
import java.net.{Socket, InetAddress}

/**
 * Created by fafa on 26/2/15.
 */
object TcpTest {

  def main(args:Array[String]): Unit ={
    val socket = new Socket("localhost",5858)
    val out = new PrintStream(socket.getOutputStream)

    while(true) {
      val value = Math.random()
      out.println(s"addRecord:1;1;$value")
      out.flush()
      Thread.sleep(1000)
    }
  }

}
