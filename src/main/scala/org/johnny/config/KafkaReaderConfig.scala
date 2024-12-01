package org.johnny.config

case class KafkaReaderConfig(kafkaBootstrapServers: String,
                             topics: String = "ODS_ORDER_LOG",
                             startingOffsets: String = "latest",
                             failOnDataLoss: Boolean = false,
                             maxOffsetsPerTrigger: Int = 3000)
