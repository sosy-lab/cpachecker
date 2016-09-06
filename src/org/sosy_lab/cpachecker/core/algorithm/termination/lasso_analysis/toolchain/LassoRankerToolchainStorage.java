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
import com.google.common.collect.Maps;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;

import java.util.Map;
import java.util.logging.Level;

import de.uni_freiburg.informatik.ultimate.core.model.IServiceFactory;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.services.IBacktranslationService;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILoggingService;
import de.uni_freiburg.informatik.ultimate.core.model.services.IProgressMonitorService;
import de.uni_freiburg.informatik.ultimate.core.model.services.IResultService;
import de.uni_freiburg.informatik.ultimate.core.model.services.IService;
import de.uni_freiburg.informatik.ultimate.core.model.services.IStorable;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;

public class LassoRankerToolchainStorage implements IToolchainStorage, IUltimateServiceProvider {

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ILoggingService lassoRankerLoggingService;
  private final Map<String, IStorable> toolchainStorage;

  public LassoRankerToolchainStorage(LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    logger = Preconditions.checkNotNull(pLogger);
    shutdownNotifier = Preconditions.checkNotNull(pShutdownNotifier);
    lassoRankerLoggingService = new LassoRankerLoggingService(pLogger);
    toolchainStorage = Maps.newConcurrentMap();
  }

  @Override
  public void destroyStorable(String pKey) {
    final IStorable storable = removeStorable(pKey);
    if (storable != null) {
      storable.destroy();
    }
  }

  @Override
  public IStorable removeStorable(String pKey) {
    return toolchainStorage.remove(pKey);
  }

  @Override
  public IStorable getStorable(String pKey) {
    return toolchainStorage.get(pKey);
  }

  @Override
  public IStorable putStorable(String pKey, IStorable pValue) {
    return toolchainStorage.put(pKey, pValue);
  }

  @Override
  public void clear() {
    for (final IStorable storable : toolchainStorage.values()) {
      try {
        storable.destroy();
      } catch (final Throwable t) {
        logger.logException(
            Level.WARNING,
            t,
            "Exception during clearing of toolchain storage while destroying " + storable);
      }
    }
    toolchainStorage.clear();
  }

  @Override
  public IBacktranslationService getBacktranslationService() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ILoggingService getLoggingService() {
    return lassoRankerLoggingService;
  }

  @Override
  public IResultService getResultService() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IProgressMonitorService getProgressMonitorService() {
    return new LassoRankerProgressMonitorService(shutdownNotifier);
  }

  @Override
  public <T extends IService, K extends IServiceFactory<T>> T getServiceInstance(
      Class<K> pServiceType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IPreferenceProvider getPreferenceProvider(String pPluginId) {
    throw new UnsupportedOperationException();
  }
}
