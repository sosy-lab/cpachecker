// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import com.google.common.base.Preconditions;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
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
      ConfigurableProgramAnalysis pCpa, LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    super(pCpa);
    Preconditions.checkArgument(
        pCpa instanceof PredicateCPA, "Child-CPA is required to be an instance of PredicateCPA");

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    itpSequenceStorage = new InterpolationSequenceStorage();

    predicateManager = ((PredicateCPA) pCpa).getPredicateManager();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return TraceAbstractionState.createInitState(super.getInitialState(pNode, pPartition));
  }

  @SuppressWarnings("resource")
  @Override
  public TransferRelation getTransferRelation() {
    PredicateCPA wrappedCpa = (PredicateCPA) getWrappedCpa();

    return new TraceAbstractionTransferRelation(
        wrappedCpa.getTransferRelation(),
        wrappedCpa.getSolver().getFormulaManager(),
        predicateManager,
        itpSequenceStorage,
        logger,
        shutdownNotifier);
  }

  InterpolationSequenceStorage getInterpolationSequenceStorage() {
    return itpSequenceStorage;
  }
}
