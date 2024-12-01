package org.johnny.bean

import org.apache.spark.sql.types._

object OrderSchema {
  val schema: StructType = StructType(List(
    StructField("order_id", StringType), // Unique identifier for the order
    StructField("user_id", StringType), // Unique identifier for the user
    StructField("product_id", StringType), // Unique identifier for the product
    StructField("product_name", StringType), // Name of the product
    StructField("category", StringType), // Category of the product
    StructField("price", DoubleType), // Price of a single unit
    StructField("quantity", IntegerType), // Quantity of the product ordered
    StructField("order_date", DateType), // Date when the order was placed
    StructField("order_status", StringType), // Current status of the order
    StructField("delivery_date", DateType), // Date when the order was delivered
    StructField("updated_time", TimestampType)
  ))
}
