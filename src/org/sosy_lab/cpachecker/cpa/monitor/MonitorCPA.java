// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.monitor;

import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

public class MonitorCPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(MonitorCPA.class);
  }

  private final MonitorTransferRelation transferRelation;
  private final Statistics stats;

  private MonitorCPA(ConfigurableProgramAnalysis pCpa, Configuration config)
      throws InvalidConfigurationException {
    super(pCpa);
    transferRelation = new MonitorTransferRelation(getWrappedCpa(), config);
    stats = new MonitorStatistics(this);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new FlatLatticeDomain();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new MonitorState(getWrappedCpa().getInitialState(pNode, pPartition), 0L);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new MonitorMerge(getWrappedCpa());
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new MonitorPrecisionAdjustment(getWrappedCpa().getPrecisionAdjustment());
  }

  @Override
  public StopOperator getStopOperator() {
    return new MonitorStop(getWrappedCpa());
  }

  @Override
  public MonitorTransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }
}
