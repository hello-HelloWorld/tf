package modeling

import java.io.{File, PrintWriter}
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.{SparkConf, SparkContext}
import redis.clients.jedis.Jedis
import utils.RedisUtil

import scala.collection.mutable.ArrayBuffer

/*
* @author: sunxiaoxiong
* @date  : Created in 2020/5/7 11:10
*/
/*
堵车预测：数据建模

a) 确定要对哪个监测点进行建模，我们称之为目标监测点
b) 找到目标监测点的其他相关监测点（比如相关监测点与目标监测点属于一条公路的）
c) 从redis中访问得到以上所有监测点若干小时内的历史数据信息（一部分作为训练数据，一部分作为测试数据）
d) 提取组装特征向量与目标向量，训练参数集，训练模型
e) 测试模型吻合度，将符合吻合度的模型保存到HDFS中，同时将模型的保存路径放置于redis中
* */
object Train {

  //写入文件的输出流
  val writer = new PrintWriter(new File("model_train.txt"))
  //初始化spark
  val sparkConf = new SparkConf().setMaster("local[*]").setAppName("train")
  val sc = new SparkContext(sparkConf)

  //定义redis数据库的相关
  val dbIndex = 1
  //获取jedis连接
  val jedis: Jedis = RedisUtil.pool.getResource
  jedis.select(dbIndex)

  //设立目标监测点：你要对哪几个监测点进行建模
  val monitorIDs = List("0005", "0015")
  //取出相关检测点
  val monitorRelations = Map[String, Array[String]]("0005" -> Array("0003", "0004", "0005", "0006", "0007"), "0015" -> Array("0014", "0014", "0015", "0016", "0017"))

  //遍历上面所有的目标检测点，读取数据
  monitorIDs.map(monitorID => {
    //得到当前检测点的相关监测点
    val monitorRelationArray: Array[String] = monitorRelations(monitorID)

    //初始化时间
    val currentDate: Date = Calendar.getInstance().getTime
    //当前小时分钟数
    val hourMinuteSDF: SimpleDateFormat = new SimpleDateFormat("HHmm")
    //当前年月日
    val dateSDF: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
    //以当前时间，格式化好年月日的时间
    val dateOfString: String = dateSDF.format(currentDate)

    //根据“相关监测点”，取得当日的所有的监测点的平均车速
    //最终结果样式：(0005, {1033=93_2, 1034=1356_30})
    val relationsInfo = monitorRelationArray.map(monitorID => {
      (monitorID, jedis.hgetAll(dateOfString + "_" + monitorID))
    })

    //确定使用多少小时内的数据进行建模
    val hours = 1
    //创建3个数组，一个数组用于存放特征向量，一数组用于存放Label向量，一个数组用于存放前两者之间的关联
    val dataX: ArrayBuffer[Double] = ArrayBuffer[Double]()
    val dataY: ArrayBuffer[Double] = ArrayBuffer[Double]()

    //用于存放特征向量和特征结果的映射关系
    val dataTrain: ArrayBuffer[LabeledPoint] = ArrayBuffer[LabeledPoint]()

    //将时间拉回到1小时之前，倒序，拉回单位：分钟
    for (i <- Range(60 * hours, 2, -1)) {
      dataX.clear()
      dataY.clear()
      //一下内容包含：线性滤波
      for (index <- 0 to 2) {
        //当前毫秒数 - 1个小时之前的毫秒数 + 1个小时之前的后0分钟，1分钟，2分钟的毫秒数（第3分钟作为Label向量）
        val oneMoment = currentDate.getTime - 60 * i * 1000 + 60 * index * 1000
        //拼装出当前（当前for循环这一次的时间）的小时分钟数
        val oneHM = hourMinuteSDF.format(new Date(oneMoment)) //1642 field
        //取出该时刻下里面数据
        //取出的数据形式为：(0005, {1033=93_2, 1034=1356_30})
        for ((k, v) <- relationsInfo) {
          //如果index==2，意味着前3分钟的数据已经组装到了dataX中，那么下一时刻的数据，如果是目标卡口，则需要存放于dataY中
          if (k == monitorID && index == 2) {

          }
        }
      }
    }

  })
}
