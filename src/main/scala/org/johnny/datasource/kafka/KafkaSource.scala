package org.johnny.datasource.kafka

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.johnny.config.KafkaReaderConfig
import org.johnny.datasource.DataSource

class KafkaSource(kafkaReaderConfig: KafkaReaderConfig) extends DataSource{
  def readStream(spark: SparkSession): DataFrame = {
    spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaReaderConfig.kafkaBootstrapServers)
      .option("subscribe", kafkaReaderConfig.topics)
      .option("startingOffsets", kafkaReaderConfig.startingOffsets)
      .option("failOnDataLoss", kafkaReaderConfig.failOnDataLoss)
      .option("maxOffsetsPerTrigger", kafkaReaderConfig.maxOffsetsPerTrigger)
      .load()
  }
}
