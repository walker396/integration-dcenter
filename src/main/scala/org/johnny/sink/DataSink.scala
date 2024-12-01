package org.johnny.sink

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.streaming.StreamingQuery

trait DataSink {
  def writeStream(dataFrame: DataFrame): StreamingQuery
}
