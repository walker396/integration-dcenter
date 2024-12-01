package org.johnny.processing.transformations

import org.apache.spark.sql.{DataFrame, functions => F}
import org.apache.spark.sql.types.StructType

class DataTransformer(orderSchema: StructType) {
  def transform(df: DataFrame): DataFrame = {
    df.selectExpr("CAST(value AS STRING) as json_value")
      .withColumn("data", F.from_json(F.col("json_value"), orderSchema))
      .select("data.*")
  }
}
