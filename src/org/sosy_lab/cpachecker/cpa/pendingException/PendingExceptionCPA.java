// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pendingException;

import java.util.Collection;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

public class PendingExceptionCPA extends AbstractCPA implements StatisticsProvider {

  private final PendingExceptionCPAStatistics statistics;
  private final LogManager logger;

  public PendingExceptionCPA(LogManager pLogger) {
    super(
        "sep",
        "sep",
        DelegateAbstractDomain.<PendingExceptionState>getInstance(),
        new PendingExceptionTransferRelation());
    this.logger = pLogger;
    statistics = new PendingExceptionCPAStatistics();
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PendingExceptionCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new PendingExceptionState();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

  public PendingExceptionCPAStatistics getStats() {
    return statistics;
  }

  public LogManager getLogger() {
    return logger;
  }
}
