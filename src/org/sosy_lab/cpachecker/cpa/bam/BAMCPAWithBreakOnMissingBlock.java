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
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCacheSynchronized;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManagerSynchronized;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "cpa.bam")
public class BAMCPAWithBreakOnMissingBlock extends AbstractBAMCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BAMCPAWithBreakOnMissingBlock.class);
  }

  @Option(
    secure = true,
    description = "abort current analysis when finding a missing block abstraction"
  )
  private boolean breakForMissingBlock = true;

  private final BAMCache cache;
  private final BAMDataManager data;

  private BAMCPAWithBreakOnMissingBlock(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory reachedsetFactory,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException, CPAException {
    super(pCpa, pConfig, pLogger, pShutdownNotifier, pSpecification, pCfa);
    pConfig.inject(this);

    cache = new BAMCacheSynchronized(pConfig, getReducer(), pLogger);
    data = new BAMDataManagerSynchronized(cache, reachedsetFactory, pLogger);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new BAMTransferRelationWithBreakOnMissingBlock(this, shutdownNotifier);
  }

  @Override
  public BAMPrecisionAdjustment getPrecisionAdjustment() {
    return new BAMPrecisionAdjustmentWithBreakOnMissingBlock(
        getWrappedCpa().getPrecisionAdjustment(),
        data,
        logger,
        blockPartitioning,
        breakForMissingBlock);
  }

  @Override
  public StopOperator getStopOperator() {
    return new BAMStopOperatorWithBreakOnMissingBlock(getWrappedCpa().getStopOperator());
  }

  public BAMCache getCache() {
    return cache;
  }

  @Override
  public BAMDataManager getData() {
    return data;
  }

  public boolean doesBreakForMissingBlock() {
    return breakForMissingBlock;
  }
}
