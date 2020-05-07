package com.mozilla.telemetry.ingestion.sink.util;

import java.util.function.Supplier;
import org.slf4j.Logger;

public class LoggedException extends RuntimeException {

  private LoggedException(RuntimeException e) {
    super(e);
  }

  /**
   * Return an already logged exception. Log any other exception and wrap it in
   * {@link LoggedException} so it won't be logged again.
   */
  public static RuntimeException logAndWrap(Logger logger, Supplier<String> message,
      RuntimeException e) {
    if (log(logger, message, e)) {
      return new LoggedException(e);
    }
    return e;
  }

  /** Log an exception unless it was already logged and return whether it was logged. */
  public static boolean log(Logger logger, Supplier<String> message, RuntimeException e) {
    if (e instanceof LoggedException) {
      return false;
    }
    logger.error(message.get(), e);
    return true;
  }
}
