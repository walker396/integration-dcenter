package org.johnny.processing

import org.apache.spark.sql.DataFrame

trait DataProcessor {
  def process(input: DataFrame): DataFrame
}
