// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
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
      description = "abort current analysis when finding a missing block abstraction")
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
    data = new BAMDataManagerSynchronized(this, cache, reachedsetFactory, pLogger);
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
