// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.toolchain;

import com.google.common.base.Preconditions;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger.LogLevel;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILoggingService;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import java.io.Writer;
import org.sosy_lab.common.log.LogManager;

public class LassoRankerLoggingService implements ILoggingService {

  private final LogManager logger;

  public LassoRankerLoggingService(LogManager pLogger) {
    logger = Preconditions.checkNotNull(pLogger);
  }

  @Override
  public ILogger getLogger(String pPluginId) {
    return new LassoRankerLogger(logger, pPluginId);
  }

  @Override
  public ILogger getLogger(Class<?> pClazz) {
    return new LassoRankerLogger(logger, pClazz.getSimpleName());
  }

  @Override
  public ILogger getLoggerForExternalTool(String pId) {
    return new LassoRankerLogger(logger, pId);
  }

  @Override
  public ILogger getControllerLogger() {
    return new LassoRankerLogger(logger);
  }

  @Override
  public Object getBacking(ILogger pLogger, Class<?> pBackingType) {
    throw new UnsupportedOperationException(
        getClass() + "::getBacking(ILogger, Class<?>) is not implemented");
  }

  @Override
  public void addWriter(Writer pWriter, String pLogPattern) {
    throw new UnsupportedOperationException(
        getClass() + "::addWriter(Writer, String) is not implemented");
  }

  @Override
  public void removeWriter(Writer pWriter) {
    throw new UnsupportedOperationException(
        getClass() + "::removeWriter(Writer) is not implemented");
  }

  @Override
  public void setLogLevel(Class<?> pClazz, LogLevel pLevel) {
    throw new UnsupportedOperationException(
        getClass() + "::setLogLevel(Class<?>, LogLevel) is not implemented");
  }

  @Override
  public void setLogLevel(String pId, LogLevel pLevel) {
    throw new UnsupportedOperationException(
        getClass() + "::setLogLevel(String, LogLevel) is not implemented");
  }

  @Override
  public void reloadLoggers() {
    throw new UnsupportedOperationException(getClass() + "::reloadLoggers() is not implemented");
  }

  @Override
  public void setCurrentControllerID(String pName) {
    throw new UnsupportedOperationException(
        getClass() + "::setCurrentControllerID(String) is not implemented");
  }

  @Override
  public void store(IToolchainStorage pStorage) {
    throw new UnsupportedOperationException(getClass() + "::store() is not implemented");
  }
}
