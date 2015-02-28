import java.io.PrintStream
import java.net.{DatagramPacket, DatagramSocket, Socket, InetAddress}

/**
 * Created by fafa on 26/2/15.
 */
object TcpTest {

  def main(args:Array[String]): Unit ={
    val socket = new Socket("localhost",5858)
    val out = new PrintStream(socket.getOutputStream)

    while(true) {
      val value = Math.random()
      val vtype = if(Math.random() >0.5) 1 else 0
      out.println(s"addRecord:1;$vtype;$value")
      out.flush()
      Thread.sleep(1000)
    }
  }
}

object UdpTest {

  def main(args:Array[String]): Unit ={
    val socket = new DatagramSocket()

    while(true) {
      val value = Math.random()
      val vtype = if(Math.random() >0.5) 1 else 0
      val ia:InetAddress= InetAddress.getLocalHost;
      val msg = s"addRecord:1;$vtype;$value".getBytes("US-ASCII")
      val packet:DatagramPacket = new DatagramPacket(msg,msg.length,ia,5858)
      socket.send(packet)
    }
  }

}
