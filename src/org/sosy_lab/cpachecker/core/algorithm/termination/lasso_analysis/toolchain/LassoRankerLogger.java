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
    return switch (pLevel) {
      case DEBUG -> logger.wouldBeLogged(Level.ALL);
      case ERROR -> logger.wouldBeLogged(Level.WARNING);
      case FATAL -> logger.wouldBeLogged(Level.SEVERE);
      case INFO -> logger.wouldBeLogged(Level.FINEST);
      case OFF -> logger.wouldBeLogged(Level.OFF);
      case WARN -> logger.wouldBeLogged(Level.INFO);
    };
  }

  @Override
  public void log(LogLevel pLevel, String pMessage) {
    switch (pLevel) {
      case DEBUG -> debug(pMessage);
      case ERROR -> error(pMessage);
      case FATAL -> fatal(pMessage);
      case INFO -> info(pMessage);
      case WARN -> warn(pMessage);
      case OFF -> {
        // logging disabled
      }
    }
  }

  @Override
  public void setLevel(LogLevel pLevel) {
    throw new UnsupportedOperationException(getClass() + "::setLevel is not implemented");
  }

  private void logException(Level level, Object pMessage, Throwable pThrowable) {
    if (pThrowable != null) {
      logger.logException(level, pThrowable, pMessage.toString());
    } else {
      logger.log(level, pMessage);
    }
  }
}
