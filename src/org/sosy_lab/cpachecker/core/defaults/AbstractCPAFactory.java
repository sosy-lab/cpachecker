/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

import java.util.List;

import javax.annotation.Nullable;

public abstract class AbstractCPAFactory implements CPAFactory {

  private @Nullable LogManager logger = null;
  private @Nullable Configuration configuration = null;
  private @Nullable ShutdownNotifier shutdownNotifier = null;

  @Override
  public CPAFactory setChild(ConfigurableProgramAnalysis pChild)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Cannot wrap CPA");
  }

  @Override
  public CPAFactory setChildren(List<ConfigurableProgramAnalysis> pChildren)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Cannot wrap CPAs");
  }

  @Override
  public CPAFactory setConfiguration(Configuration pConfiguration) {
    Preconditions.checkNotNull(pConfiguration);
    Preconditions.checkState(configuration == null, "setConfiguration called twice on CPAFactory");

    configuration = pConfiguration;
    return this;
  }

  @Override
  public CPAFactory setLogger(LogManager pLogger) {
    Preconditions.checkNotNull(pLogger);
    Preconditions.checkState(logger == null, "setLogger called twice on CPAFactory");

    logger = pLogger;
    return this;
  }

  @Override
  public CPAFactory setShutdownNotifier(ShutdownNotifier pShutdownNotifier) {
    Preconditions.checkNotNull(pShutdownNotifier);
    Preconditions.checkState(shutdownNotifier == null, "setShutdownNotifier called twice on CPAFactory");

    shutdownNotifier = pShutdownNotifier;
    return this;
  }

  protected LogManager getLogger() {
    Preconditions.checkState(logger != null, "LogManager object needed to create CPA");
    return logger;
  }

  protected Configuration getConfiguration() {
    Preconditions.checkState(configuration != null, "Configuration object needed to create CPA");
    return configuration;
  }

  public ShutdownNotifier getShutdownNotifier() {
    Preconditions.checkState(shutdownNotifier != null, "ShutdownNotifier object needed to create CPA");
    return shutdownNotifier;
  }

  @Override
  public <T> CPAFactory set(T pObject, Class<T> pClass) throws UnsupportedOperationException {
    // ignore other objects
    return this;
  }
}
