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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.toolchain;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.log.LogManager;

import java.io.Writer;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILoggingService;

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
    throw new UnsupportedOperationException();
  }

  @Override
  public void addWriter(Writer pWriter, String pLogPattern) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeWriter(Writer pWriter) {
    throw new UnsupportedOperationException();
  }
}
