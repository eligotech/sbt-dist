package com.eligotech.sbt.plugins

import sbt.{Level, ConsoleLogger}


trait Logging {

  private val logger = ConsoleLogger()

  def logInfo = logger.log(Level.Info, _: String)
  def logDebug = logger.log(Level.Debug, _: String)
  def logError = logger.log(Level.Error, _: String)

}
