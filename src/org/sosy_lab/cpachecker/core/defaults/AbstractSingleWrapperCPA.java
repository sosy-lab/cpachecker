/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

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

import java.util.Collection;

/**
 * Base class for CPAs which wrap exactly one other CPA.
 */
public abstract class AbstractSingleWrapperCPA implements ConfigurableProgramAnalysis, WrapperCPA, StatisticsProvider {

  private final ConfigurableProgramAnalysis wrappedCpa;

  public AbstractSingleWrapperCPA(ConfigurableProgramAnalysis pCpa) {
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
      ((StatisticsProvider)wrappedCpa).collectStatistics(pStatsCollection);
    }
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(wrappedCpa.getClass())) {
      return pType.cast(wrappedCpa);
    } else if (wrappedCpa instanceof WrapperCPA) {
      return ((WrapperCPA)wrappedCpa).retrieveWrappedCpa(pType);
    } else {
      return null;
    }
  }

  @Override
  public ImmutableList<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return ImmutableList.of(wrappedCpa);
  }
}