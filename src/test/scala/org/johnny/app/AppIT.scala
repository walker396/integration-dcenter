package org.johnny.app

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializeConfig
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.commons.io.FileUtils
import org.apache.iceberg.Table
import org.apache.iceberg.spark.Spark3Util
import org.awaitility.Awaitility.await
import org.awaitility.scala.AwaitilitySupport
import org.johnny.bean.{Order, OrderSchema}
import org.johnny.config.{IcebergConfig, KafkaReaderConfig}
import org.johnny.job.StreamingJobExecutor
import org.johnny.utils.IcebergUtil
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.slf4j.LoggerFactory

import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

class AppIT extends AnyFlatSpec with Matchers with EmbeddedKafka with AwaitilitySupport {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val testOrders: Seq[String] = Seq(
    """{"order_id":"ORD021","user_id":"USR001","product_id":"PRD001","product_name":"Wireless Mouse","category":"Electronics","price":25.99,"quantity":2,"order_date":"2024-11-15","order_status":"Pending","delivery_date":"2024-11-17","updated_time":"2024-11-17 12:20:00"}""",
    """{"order_id":"ORD022","user_id":"USR002","product_id":"PRD002","product_name":"Bluetooth Keyboard","category":"Electronics","price":45.99,"quantity":1,"order_date":"2024-11-15","order_status":"Completed","delivery_date":"2024-11-18","updated_time":"2024-11-17 15:20:00"}"""
  )

  "runs with embedded kafka" should "work" in {
    implicit val userDefinedConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = 9093, zooKeeperPort = 2181)

    withRunningKafka {
      FileUtils.deleteDirectory(new File("src/main/resources/warehouse/catalog/demo"))
      val kafkaReaderConfig = KafkaReaderConfig("localhost:" + userDefinedConfig.kafkaPort)
      val icebergConfig = IcebergConfig()
      val spark = IcebergUtil.initializeSparkSessionByIceberg(icebergConfig)

      val executor = new StreamingJobExecutor(spark, kafkaReaderConfig, icebergConfig, OrderSchema.schema)
      val streamingThread = new Thread(() => executor.execute(200000))
      streamingThread.start()
      Thread.sleep(30000)
      val kafkaTopic = kafkaReaderConfig.topics

      logger.info(s"----start testing----${new Date()}")


      def queryOrderCount(orderIds: Seq[String]): Long = {
        val table: Table = Spark3Util.loadIcebergTable(spark, "demo.orders")
        table.refresh()
        spark.sql(s"SELECT COUNT(*) as orderCount FROM demo.orders WHERE order_id IN (${orderIds.map(id => s"'$id'").mkString(",")})")
          .first().getAs[Long]("orderCount")
      }

      def queryOrderStatus(orderId: String): String = {
        val table: Table = Spark3Util.loadIcebergTable(spark, "demo.orders")
        table.refresh()
        spark.sql(s"SELECT order_status FROM demo.orders WHERE order_id = '$orderId'")
          .first().getAs[String]("order_status")
      }

      def queryEventsSequencing(): Seq[String] = {
        val table: Table = Spark3Util.loadIcebergTable(spark, "demo.orders")
        table.refresh()
        spark.sql("SELECT order_id FROM demo.orders ORDER BY updated_time DESC LIMIT 2")
          .collect().map(_.getString(0)).toSeq
      }

      def queryDataIntegrity(orderId: String, productName: String): Long = {
        spark.sql(s"SELECT COUNT(*) as orderCount FROM demo.orders WHERE order_id = '$orderId' AND product_name = '$productName'")
          .first().getAs[Long]("orderCount")
      }

      // Test case 1: Validate initial insertion
      testOrders.foreach(order => publishStringMessageToKafka(kafkaTopic, order))
      await("After insertion, there should be 2 records.") atMost(20, TimeUnit.SECONDS) until {
        queryOrderCount(Seq("ORD021", "ORD022")) == 2
      }

      // Test case 2: Validate duplicate record handling
      val duplicatedOrder = new Order("ORD021", "USR001", "PRD001", "Wireless Mouse", "Electronics", 25.99, 2, "2024-11-15", "Pending", "2024-11-17", "2024-11-17 15:21:00")
      logger.info(s"Test Insert one duplicated order: ${new Date()}")
      publishStringMessageToKafka(kafkaTopic, JSON.toJSONString(duplicatedOrder, new SerializeConfig(true)))

      await("Only one record for duplicated order") atMost(20, TimeUnit.SECONDS) until {
        queryOrderCount(Seq("ORD021")) == 1
      }

      // Test case 3: Validate status update
      val updatedOrder = new Order("ORD021", "USR001", "PRD001", "Wireless Mouse", "Electronics", 25.99, 2, "2024-11-15", "Completed", "2024-11-17", "2024-11-17 15:22:00")
      logger.info(s"Test update the status of one order: ${new Date()}")
      publishStringMessageToKafka(kafkaTopic, JSON.toJSONString(updatedOrder, new SerializeConfig(true)))

      await("The order status should change to Completed") atMost(20, TimeUnit.SECONDS) until {
        queryOrderStatus("ORD021") == "Completed"
      }

      // Test case 4: Validate event sequencing
      val seqOrders = Seq(
        """{"order_id":"ORD041","user_id":"USR003","product_id":"PRD003","product_name":"Wireless Mouse3","category":"Electronics","price":125.99,"quantity":1,"order_date":"2024-11-17","order_status":"Pending","delivery_date":"2024-11-25","updated_time":"2024-11-17 15:24:00"}""",
        """{"order_id":"ORD042","user_id":"USR004","product_id":"PRD004","product_name":"Bluetooth Keyboard4","category":"Electronics","price":145.99,"quantity":1,"order_date":"2024-11-17","order_status":"Pending","delivery_date":"2024-11-25","updated_time":"2024-11-17 15:25:00"}"""
      )
      seqOrders.foreach(order => publishStringMessageToKafka(kafkaTopic, order))

      await("Events sequencing should be consistent.") atMost(20, TimeUnit.SECONDS) until {
        queryEventsSequencing() == Seq("ORD042", "ORD041")
      }

      // Test case 5: Validate data integrity
      logger.info(s"Test Data Integrity: ${new Date()}")
      await("The data fields should match exactly.") atMost(20, TimeUnit.SECONDS) until {
        queryDataIntegrity("ORD041", "Wireless Mouse3") == 1
      }
      spark.stop()
      streamingThread.stop()
    }
  }
}


