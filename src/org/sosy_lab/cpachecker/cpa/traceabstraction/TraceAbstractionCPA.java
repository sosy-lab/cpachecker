// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;

public class TraceAbstractionCPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TraceAbstractionCPA.class);
  }

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final InterpolationSequenceStorage itpSequenceStorage;

  private final PredicateAbstractionManager predicateManager;

  @SuppressWarnings("resource")
  private TraceAbstractionCPA(
      ConfigurableProgramAnalysis pCpa, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(pCpa);
    if (!(pCpa instanceof PredicateCPA predicateCPA)) {
      throw new InvalidConfigurationException(
          "TraceAbstractionCPA is a wrapper CPA that requires the contained CPA to be an "
              + "instance of PredicateCPA, but configured was a "
              + pCpa.getClass().getSimpleName());
    }

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    itpSequenceStorage = new InterpolationSequenceStorage();

    predicateManager = predicateCPA.getPredicateManager();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return TraceAbstractionState.createInitState(super.getInitialState(pNode, pPartition));
  }

  @SuppressWarnings("resource")
  @Override
  public TransferRelation getTransferRelation() {
    return new TraceAbstractionTransferRelation(super.getTransferRelation());
  }

  InterpolationSequenceStorage getInterpolationSequenceStorage() {
    return itpSequenceStorage;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new TraceAbstractionAbstractDomain(super.getAbstractDomain());
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new TraceAbstractionMergeOperator(super.getMergeOperator());
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  @SuppressWarnings("resource")
  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    PredicateCPA predicateCpa = (PredicateCPA) getWrappedCpa();

    return new TraceAbstractionPrecisionAdjustment(
        predicateCpa.getPrecisionAdjustment(),
        predicateCpa.getSolver().getFormulaManager(),
        predicateManager,
        predicateCpa.getAbstractionManager(),
        itpSequenceStorage,
        logger,
        shutdownNotifier);
  }
}
