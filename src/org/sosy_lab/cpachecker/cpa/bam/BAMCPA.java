// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm.CEGARAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCacheAggressiveImpl;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCacheImpl;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManagerImpl;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix = "cpa.bam")
public class BAMCPA extends AbstractBAMCPA implements StatisticsProvider, ProofChecker {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BAMCPA.class);
  }

  private final BAMTransferRelation transfer;
  private final ProofChecker wrappedProofChecker;
  private final BAMDataManager data;
  private final BAMPCCManager bamPccManager;

  @Option(
      name = "handleRecursiveProcedures",
      secure = true,
      description =
          "BAM allows to analyse recursive procedures. This strongly depends on the underlying CPA."
              + " The current support includes only ValueAnalysis and PredicateAnalysis (with tree"
              + " interpolation enabled).")
  private boolean handleRecursiveProcedures = false;

  @Option(
      secure = true,
      description =
          "If enabled, cache queries also consider blocks with non-matching precision for reuse.")
  private boolean aggressiveCaching = true;

  @Option(
      secure = true,
      description = "Should the nested CPA-algorithm be wrapped with CEGAR within BAM?")
  private boolean useCEGAR = false;

  private BAMCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException, CPAException {
    super(pCpa, config, pLogger, pShutdownNotifier, pSpecification, pCfa);
    config.inject(this);

    if (pCpa instanceof ProofChecker) {
      wrappedProofChecker = (ProofChecker) pCpa;
    } else {
      wrappedProofChecker = null;
    }

    final BAMCache cache;
    if (aggressiveCaching) {
      cache = new BAMCacheAggressiveImpl(config, getReducer(), logger);
    } else {
      cache = new BAMCacheImpl(config, getReducer(), logger);
    }
    data = new BAMDataManagerImpl(this, cache, pReachedSetFactory, pLogger);

    bamPccManager =
        new BAMPCCManager(wrappedProofChecker, config, blockPartitioning, getReducer(), this, data);

    AlgorithmFactory factory = new CPAAlgorithmFactory(this, logger, config, pShutdownNotifier);
    if (useCEGAR) {
      // We will use this single instance of CEGARAlgFactory for the whole analysis.
      // There will be exactly one Refiner within all nestings of BAM (and one from the surrounding
      // CEGAR loop), because it is part of the factory.
      factory = new CEGARAlgorithmFactory(factory, this, logger, config, pShutdownNotifier);
    }

    if (handleRecursiveProcedures) {
      transfer =
          new BAMTransferRelationWithFixPointForRecursion(
              config, this, pShutdownNotifier, factory, bamPccManager, searchTargetStatesOnExit());
    } else {
      transfer =
          new BAMTransferRelation(
              this, pShutdownNotifier, factory, bamPccManager, searchTargetStatesOnExit());
    }
  }

  @Override
  public BAMMergeOperator getMergeOperator() {
    return super.getMergeOperator().withBAMPCCManager(bamPccManager);
  }

  @Override
  public StopOperator getStopOperator() {
    return handleRecursiveProcedures
        ? new BAMStopOperatorForRecursion(getWrappedCpa().getStopOperator())
        : getWrappedCpa().getStopOperator();
  }

  @Override
  public BAMPrecisionAdjustment getPrecisionAdjustment() {
    return new BAMPrecisionAdjustment(
        getWrappedCpa().getPrecisionAdjustment(), data, bamPccManager, logger, blockPartitioning);
  }

  @Override
  public BAMTransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public BAMDataManager getData() {
    Preconditions.checkNotNull(data);
    return data;
  }

  public BAMPCCManager getBamPccManager() {
    return bamPccManager;
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pState, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(
        wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return bamPccManager.areAbstractSuccessors(pState, pCfaEdge, pSuccessors);
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState)
      throws CPAException, InterruptedException {
    Preconditions.checkNotNull(
        wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return wrappedProofChecker.isCoveredBy(pState, pOtherState);
  }

  public BAMMultipleCEXSubgraphComputer createBAMMultipleSubgraphComputer(
      Function<ARGState, Integer> pIdExtractor) {
    return new BAMMultipleCEXSubgraphComputer(this, pIdExtractor);
  }
}
