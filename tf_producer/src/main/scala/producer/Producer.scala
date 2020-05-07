package producer

import java.text.DecimalFormat
import java.util
import java.util.Calendar

import com.alibaba.fastjson.JSON
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import utils.PropertyUtil

import scala.util.Random


/*
* @author: sunxiaoxiong
* @date  : Created in 2020/4/29 11:26
*/
/**
  * 模拟数据生产
  * 随机产生：监测点id，车速（按照5分钟的频率变换堵车状态）
  * 序列化为Json
  * 发送给kafka
  */
object Producer {
  def main(args: Array[String]): Unit = {
    //读取kafka配置信息
    val properties = PropertyUtil.properties
    //创建kafka生产者对象
    val kafkaProducer = new KafkaProducer[String, String](properties)

    //模拟产生实时数据，单位：秒
    var startTime = Calendar.getInstance().getTimeInMillis() / 1000
    //数据模拟，堵车状态切换的周期单位为：秒
    val trafficCycle = 10
    //开始不停的产生实时数据
    val df = new DecimalFormat("0000")
    while (true) {
      //模拟产生监测点id：0001~0020
      val randomMonitorId = df.format(Random.nextInt(20) + 1)
      //模拟车速
      var randomSpeed = "000"
      //得到本条数据产生的当前时间，单位：秒
      var currentTime = Calendar.getInstance().getTimeInMillis() / 1000
      //每5分钟切换一次公路状态
      if (currentTime - startTime > trafficCycle) {
        randomSpeed = new DecimalFormat("000").format(Random.nextInt(15))
        if (currentTime - startTime > trafficCycle * 2) {
          startTime = currentTime
        }
      } else {
        randomSpeed = new DecimalFormat("000").format(Random.nextInt(30) + 31)
      }

      //创建存放生产出来的数据的map集合
      val jsonMap: util.HashMap[String, String] = new util.HashMap[String, String]()
      jsonMap.put("monitor_id", randomMonitorId)
      jsonMap.put("speed", randomSpeed)

      //序列化 {"monitor_id":"0013","speed":"043"}
      val event = JSON.toJSON(jsonMap)
      println(event)

      //发送事件到kafka集群中
      kafkaProducer.send(new ProducerRecord[String, String](PropertyUtil.getProperty("kafka.topics"), event.toString))
      Thread.sleep(100)
    }
  }
}
