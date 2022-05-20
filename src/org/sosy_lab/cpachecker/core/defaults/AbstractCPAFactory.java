// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

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
    Preconditions.checkState(
        shutdownNotifier == null, "setShutdownNotifier called twice on CPAFactory");

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
    Preconditions.checkState(
        shutdownNotifier != null, "ShutdownNotifier object needed to create CPA");
    return shutdownNotifier;
  }

  @Override
  public <T> CPAFactory set(T pObject, Class<T> pClass) throws UnsupportedOperationException {
    // ignore other objects
    return this;
  }
}
