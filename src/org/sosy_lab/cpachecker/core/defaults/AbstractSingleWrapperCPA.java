// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;

/** Base class for CPAs which wrap exactly one other CPA. */
public abstract class AbstractSingleWrapperCPA
    implements ConfigurableProgramAnalysis, WrapperCPA, StatisticsProvider {

  private final ConfigurableProgramAnalysis wrappedCpa;

  protected AbstractSingleWrapperCPA(ConfigurableProgramAnalysis pCpa) {
    Preconditions.checkNotNull(pCpa);

    wrappedCpa = pCpa;
  }

  protected ConfigurableProgramAnalysis getWrappedCpa() {
    return wrappedCpa;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return wrappedCpa.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return wrappedCpa.getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return wrappedCpa.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return wrappedCpa.getStopOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return wrappedCpa.getPrecisionAdjustment();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return wrappedCpa.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return wrappedCpa.getInitialPrecision(pNode, pPartition);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (wrappedCpa instanceof StatisticsProvider) {
      ((StatisticsProvider) wrappedCpa).collectStatistics(pStatsCollection);
    }
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(wrappedCpa.getClass())) {
      return pType.cast(wrappedCpa);
    } else if (wrappedCpa instanceof WrapperCPA) {
      return ((WrapperCPA) wrappedCpa).retrieveWrappedCpa(pType);
    } else {
      return null;
    }
  }

  @Override
  public ImmutableList<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return ImmutableList.of(wrappedCpa);
  }
}
