package org.johnny.sink.iceberg

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._

class OrderSink(tableName: String) {
  def ensureTableExists(spark: SparkSession): Unit = {
    spark.sql(
      s"""
         |CREATE TABLE IF NOT EXISTS $tableName (
         |    order_id STRING,
         |    user_id STRING,
         |    product_id STRING,
         |    product_name STRING,
         |    category STRING,
         |    price DOUBLE,
         |    quantity INT,
         |    order_date DATE,
         |    order_status STRING,
         |    delivery_date DATE,
         |    updated_time TIMESTAMP
         |)
         |USING iceberg
         |PARTITIONED BY (category)
         |TBLPROPERTIES ('format-version'='2', 'write.upsert.enabled'='true');
       """.stripMargin)
  }

  def mergeToIceberg(batchDF: DataFrame): Unit = {
    if (!batchDF.isEmpty) {
      val windowSpec = Window.partitionBy("order_id").orderBy(col("updated_time").desc_nulls_last)
      val deduplicatedDF = batchDF
        .withColumn("row_number", row_number().over(windowSpec))
        .filter(col("row_number") === 1)
        .drop("row_number")

      deduplicatedDF.writeTo(tableName).overwritePartitions()
    }
  }
}
