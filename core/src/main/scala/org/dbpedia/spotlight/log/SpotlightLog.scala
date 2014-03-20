package org.dbpedia.spotlight.log

import org.apache.commons.logging.{Log, LogFactory}
import scala.collection.mutable

trait SpotlightLog[T] {
  def _debug(c:Class[_], msg: T, args: Any*)
  def _info(c:Class[_], msg: T, args: Any*)
  def _error(c:Class[_], msg: T, args: Any*)
  def _fatal(c:Class[_], msg: T, args: Any*)
  def _trace(c:Class[_], msg: T, args: Any*)
  def _warn(c:Class[_], msg: T, args: Any*)
}

object SpotlightLog {
  def debug[T](c:Class[_], msg: T, args: Any*)(implicit instance: SpotlightLog[T]) =
    instance._debug(c, msg, args: _*)
  def info[T](c:Class[_], msg: T, args: Any*)(implicit instance: SpotlightLog[T]) =
    instance._info(c, msg, args: _*)
  def error[T](c:Class[_], msg: T, args: Any*)(implicit instance: SpotlightLog[T]) =
    instance._error(c, msg, args: _*)
  def fatal[T](c:Class[_], msg: T, args: Any*)(implicit instance: SpotlightLog[T]) =
    instance._fatal(c, msg, args: _*)
  def trace[T](c:Class[_], msg: T, args: Any*)(implicit instance: SpotlightLog[T]) =
    instance._trace(c, msg, args: _*)
  def warn[T](c:Class[_], msg: T, args: Any*)(implicit instance: SpotlightLog[T]) =
    instance._warn(c, msg, args: _*)

  implicit object StringSpotlightLog extends SpotlightLog[String] {

    val loggers = new mutable.HashMap[Class[_], Log]()

    def _debug(c:Class[_], msg: String, args: Any*) = {
      val log = loggers.getOrElseUpdate(c, LogFactory.getLog(c))

      if (log.isDebugEnabled) {
        if(args.size == 0)
          log.debug(msg)
        else
          log.debug(msg.format(args: _*))
      }
    }
    def _info(c:Class[_], msg: String, args: Any*) = {
      val log = loggers.getOrElseUpdate(c, LogFactory.getLog(c))

      if(log.isInfoEnabled) {
        if(args.size == 0)
          log.info(msg)
        else
          log.info(msg.format(args: _*))
      }
    }
    def _error(c:Class[_], msg: String, args: Any*) = {
      val log = loggers.getOrElseUpdate(c, LogFactory.getLog(c))

      if(log.isErrorEnabled) {
        if(args.size == 0)
          log.error(msg)
        else
          log.error(msg.format(args: _*))
      }
    }
    def _fatal(c:Class[_], msg: String, args: Any*) = {
      val log = loggers.getOrElseUpdate(c, LogFactory.getLog(c))

      if(log.isFatalEnabled) {
        if(args.size == 0)
          log.fatal(msg)
        else
          log.fatal(msg.format(args: _*))
      }
    }
    def _trace(c:Class[_], msg: String, args: Any*) = {
      val log = loggers.getOrElseUpdate(c, LogFactory.getLog(c))

      if(log.isTraceEnabled) {
        if(args.size == 0)
          log.trace(msg)
        else
          log.trace(msg.format(args: _*))
      }
    }
    def _warn(c:Class[_], msg: String, args: Any*) = {
      val log = loggers.getOrElseUpdate(c, LogFactory.getLog(c))

      if(log.isWarnEnabled) {
        if(args.size == 0)
          log.warn(msg)
        else
          log.warn(msg.format(args: _*))
      }
    }
  }


}