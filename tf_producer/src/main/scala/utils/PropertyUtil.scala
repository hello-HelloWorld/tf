package utils

import java.io.InputStream
import java.util.Properties

/*
* @author: sunxiaoxiong
* @date  : Created in 2020/4/29 11:29
*/

object PropertyUtil {
  val properties = new Properties()

  try {
    //加载配置属性
    val stream: InputStream = ClassLoader.getSystemResourceAsStream("kafka.properties")
    properties.load(stream)
  } catch {
    case ex: Exception => println(ex)
  } finally {
  }

  //通过key得到kafka的属性值
  def getProperty(key: String): String = properties.getProperty(key)
}
