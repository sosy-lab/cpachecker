// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.toolchain;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;

public class LassoRankerLogger implements ILogger {

  private final LogManager logger;

  public LassoRankerLogger(LogManager pLogger) {
    logger = pLogger;
  }

  public LassoRankerLogger(LogManager pLogger, String name) {
    logger = pLogger.withComponentName(name);
  }

  @Override
  public boolean isFatalEnabled() {
    return logger.wouldBeLogged(Level.SEVERE);
  }

  @Override
  public void fatal(Object pMessage, Throwable pThrowable) {
    logException(Level.SEVERE, pMessage, pThrowable);
  }

  @Override
  public void fatal(Object pMessage) {
    logger.log(Level.SEVERE, pMessage);
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.wouldBeLogged(Level.WARNING);
  }

  @Override
  public void error(Object pMessage, Throwable pThrowable) {
    logException(Level.WARNING, pMessage, pThrowable);
  }

  @Override
  public void error(Object pMessage) {
    logger.log(Level.WARNING, pMessage);
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.wouldBeLogged(Level.INFO);
  }

  @Override
  public void warn(Object pMessage) {
    logger.log(Level.INFO, pMessage);
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.wouldBeLogged(Level.FINEST);
  }

  @Override
  public void info(Object pMessage) {
    logger.log(Level.FINEST, pMessage);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.wouldBeLogged(Level.ALL);
  }

  @Override
  public void debug(Object pMessage) {
    logger.log(Level.ALL, pMessage);
  }

  @Override
  public boolean isLogLevelEnabled(LogLevel pLevel) {
    switch (pLevel) {
      case DEBUG:
        return logger.wouldBeLogged(Level.ALL);
      case ERROR:
        return logger.wouldBeLogged(Level.WARNING);
      case FATAL:
        return logger.wouldBeLogged(Level.SEVERE);
      case INFO:
        return logger.wouldBeLogged(Level.FINEST);
      case OFF:
        return logger.wouldBeLogged(Level.OFF);
      case WARN:
        return logger.wouldBeLogged(Level.INFO);
      default:
        throw new IllegalArgumentException(this.getClass() + "Unhandled loglevel");
    }
  }

  @Override
  public void log(LogLevel pLevel, String pMessage) {
    switch (pLevel) {
      case DEBUG:
        debug(pMessage);
        break;
      case ERROR:
        error(pMessage);
        break;
      case FATAL:
        fatal(pMessage);
        break;
      case INFO:
        info(pMessage);
        break;
      case WARN:
        warn(pMessage);
        break;
      case OFF:
        // logging disabled
        break;
      default:
        throw new AssertionError("Unhandled loglevel: " + pLevel);
    }
  }

  @Override
  public void setLevel(LogLevel pLevel) {
    throw new UnsupportedOperationException(this.getClass() + "::setLevel is not implemented");
  }

  private void logException(Level level, Object pMessage, Throwable pThrowable) {
    if (pThrowable != null) {
      logger.logException(level, pThrowable, pMessage.toString());
    } else {
      logger.log(level, pMessage);
    }
  }
}
