// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class TraceAbstractionCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TraceAbstractionCPA.class);
  }

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final TraceAbstractionPredicatesStorage predicatesStorage;

  private TraceAbstractionCPA(LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    super("SEP", "SEP", null);

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    predicatesStorage = new TraceAbstractionPredicatesStorage();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return TraceAbstractionState.createInitState();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new TraceAbstractionTransferRelation(predicatesStorage, logger, shutdownNotifier);
  }

  TraceAbstractionPredicatesStorage getPredicatesStorage() {
    return predicatesStorage;
  }
}
