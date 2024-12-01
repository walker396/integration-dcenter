package org.johnny.config

case class IcebergConfig(warehousePath: String = "src/main/resources/warehouse/catalog/demo/",
                         icebergPath: String = "demo.category_summary",
                         checkpointPath: String = "src/main/resources/warehouse/catalog/demo/checkpoints/",
                         triggerInterval: Long = 10)
