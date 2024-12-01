package org.johnny.job

import org.apache.spark.sql.types.StructType
import org.johnny.config.{ IcebergConfig, KafkaReaderConfig}
import org.johnny.datasource.kafka.KafkaSource
import org.johnny.processing.transformations.DataTransformer
import org.johnny.sink.iceberg.OrderSink
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.LoggerFactory

class StreamingJobExecutor(spark: SparkSession, kafkaReaderConfig: KafkaReaderConfig, icebergConfig: IcebergConfig,schema: StructType) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val kafkaSource = new KafkaSource(kafkaReaderConfig)
  private val orderSink = new OrderSink("demo.orders")
  private val transformer = new DataTransformer(schema)

  def execute(timeoutMs: Long): Unit = {
    try {
      orderSink.ensureTableExists(spark)
      val kafkaStream = kafkaSource.readStream(spark)
      logger.info(s"Starting order stream processing from topic: ${kafkaReaderConfig.topics}")

      val transformedStream = transformer.transform(kafkaStream)
      val query = transformedStream.writeStream.foreachBatch { (batchDF: DataFrame, _: Long) =>
        orderSink.mergeToIceberg(batchDF)
      }.start()

      sys.addShutdownHook {
        query.stop()
        spark.stop()
      }

      query.awaitTermination(timeoutMs)
      logger.info("Order stream processing completed.")
    } catch {
      case e: Exception =>
        logger.error("Order stream processing failed", e)
        throw e
    }
  }
}
