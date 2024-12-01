package org.johnny.utils

import java.util.ResourceBundle

object PropertiesUtil {
  val bundle: ResourceBundle = ResourceBundle.getBundle("config")
  def apply(key: String) = bundle.getString(key)
}
