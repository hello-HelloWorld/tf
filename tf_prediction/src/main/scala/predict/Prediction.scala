package predict

import java.text.SimpleDateFormat
import java.util
import java.util.Date

import org.apache.spark.mllib.classification.LogisticRegressionModel
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.{SparkConf, SparkContext}
import redis.clients.jedis.Jedis
import utils.RedisUtil

import scala.collection.mutable.ArrayBuffer

/*
* @author: sunxiaoxiong
* @date  : Created in 2020/5/12 20:02
*/

/**
  * 思路：
  * a) 用户传入想要进行预测的时间节点，读取该时间节点之前3分钟，2分钟和1分钟的数据
  * b) 此时应该已经得到了历史数据集，通过该历史数据集预测传入时间点的车流状态
  * 尖叫提示：为了方便观察测试，建议传一个历史时间点，这样可以很直观的看到预测结果是否符合期望值。
  *
  * 堵车预测
  **/
object Prediction {
  def main(args: Array[String]): Unit = {
    //定义sparkConf
    val sparkConf = new SparkConf().setMaster("local[*]").setAppName("prediction")
    //创建sparkContext
    val sc: SparkContext = new SparkContext(sparkConf)

    //时间设置，目的是：为了拼凑出redis中的key和field的字符串
    val dateSdf: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
    val hourMinutesSdf: SimpleDateFormat = new SimpleDateFormat("HHmm")
    val userSdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm")

    //定义用于传入的，想要预测是否堵车的日期
    val inputDateString = "2020-05-12 20:10"
    val inputDate: Date = userSdf.parse(inputDateString)

    //得到redis中的key，例如：20180512
    val dayOfInputDate: String = dateSdf.format(inputDate)
    val hourMinuteOfInputDate: String = hourMinutesSdf.format(inputDate) //2012

    val dbIndex = 1
    val jedis: Jedis = RedisUtil.pool.getResource
    jedis.select(dbIndex)

    //想要预测的监测点
    val monitorIDs = List("0005", "0015")
    val monitorRelations = Map[String, Array[String]](
      "0005" -> Array("0003", "0004", "0005", "0006", "0007"),
      "0015" -> Array("0013", "0014", "0015", "0016", "0017"))

    monitorIDs.map(monitorID => {
      //相关监测点
      val monitorRealtionArray: Array[String] = monitorRelations(monitorID)
      //相关检测点数据信息 (0005, {1033=93_2, 1034=1356_30})
      val relationInfo: Array[(String, util.Map[String, String])] = monitorRealtionArray.map(monitorID => {
        (monitorID, jedis.hgetAll(dayOfInputDate + "_" + monitorID))
      })

      //装载目标时间点之前3分钟的历史数据
      val dataX = ArrayBuffer[Double]()
      //组装数据的过程
      for (index <- Range(3, 0, -1)) {
        val oneMoment = inputDate.getTime - 60 * index * 1000
        val oneHM = hourMinutesSdf.format(oneMoment)

        for ((k, v) <- relationInfo) {
          if (v.containsKey(oneHM)) {
            val speedAndCarCount: Array[String] = v.get(oneHM).split("_")
            val valueX: Float = speedAndCarCount(0).toFloat / speedAndCarCount(1).toFloat
            dataX += valueX
          } else {
            //没有值，就是没有车，赋予一个不代表堵车的车速
            dataX += 60.0F
          }
        }
      }
      println(dataX)
      //加载模型
      val modelPath: String = jedis.hget("model", monitorID)
      val model: LogisticRegressionModel = LogisticRegressionModel.load(sc, modelPath)

      //预测
      val prediction: Double = model.predict(Vectors.dense(dataX.toArray))
      println(monitorID + ",堵车预估值：" + prediction + ",是否通畅：" + (if (prediction >= 3) "通畅" else "拥堵"))
    })
    RedisUtil.pool.returnResource(jedis)
  }
}
