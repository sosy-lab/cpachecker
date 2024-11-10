// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concolic;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.statistics.StatisticsState.StatisticsStateFactory;
import org.sosy_lab.cpachecker.cpa.statistics.StatisticsState.StatisticsStateFactory.FactoryAnalysisType;
import org.sosy_lab.cpachecker.cpa.statistics.StatisticsTransferRelation;

public class StatisticsCPA extends AbstractCPA {
  protected StatisticsCPA() {
    super("sep", "sep", new StatisticsTransferRelation());
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) throws InterruptedException {
    return new StatisticsStateFactory(FactoryAnalysisType.Analysis).createNew(node);
  }
}