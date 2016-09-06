/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.lasso_ranker.toolchain;

import org.sosy_lab.common.log.LogManager;

import java.util.logging.Level;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

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

  private void logException(Level level, Object pMessage, Throwable pThrowable) {
    if (pThrowable != null) {
      logger.logException(level, pThrowable, pMessage.toString());
    } else {
      logger.log(level, pMessage);
    }
  }

}
