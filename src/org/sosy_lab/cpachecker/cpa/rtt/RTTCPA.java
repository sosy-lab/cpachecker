// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rtt;

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

import java.util.Collection;

public class RTTCPA extends AbstractCPA implements StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(RTTCPA.class);
  }

  private final RTTCPAStatistics statistics;
  private final LogManager logger;

  public RTTCPA(LogManager logger) {
    super("sep", "sep", DelegateAbstractDomain.<RTTState>getInstance(), new RTTTransferRelation());
    this.logger = logger;
    statistics = new RTTCPAStatistics();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new RTTState();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

  public RTTCPAStatistics getStats() {
    return statistics;
  }

  public LogManager getLogger() {
    return logger;
  }

}
