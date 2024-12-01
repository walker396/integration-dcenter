package org.johnny.datasource

import org.apache.spark.sql.{DataFrame, SparkSession}

trait DataSource {
  def readStream(spark: SparkSession): DataFrame
}
