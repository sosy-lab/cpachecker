/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BAMCPA2 extends AbstractBAMCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BAMCPA2.class);
  }

  private final BAMCache cache;
  private final BAMDataManager data;
  private final Reducer reducer;

  public BAMCPA2(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      ReachedSetFactory reachedsetFactory)
      throws InvalidConfigurationException, CPAException {
    super(pCpa, pConfig, pLogger, pShutdownNotifier, pCfa);

    reducer = getWrappedCpa().getReducer();
    cache = new BAMCacheSynchronized(new BAMCacheImpl(pConfig, reducer, pLogger));
    data = new BAMDataManager(cache, reachedsetFactory, pLogger);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new BAM2TransferRelation(
        blockPartitioning,
        shutdownNotifier,
        getWrappedCpa().getTransferRelation(),
        reducer,
        data,
        logger);
  }

  public BAMCache getCache() {
    return cache;
  }

  @Override
  public BAMDataManager getData() {
    return data;
  }
}
