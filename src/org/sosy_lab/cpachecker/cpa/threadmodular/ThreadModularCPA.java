/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.threadmodular;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisTM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class ThreadModularCPA
    implements ConfigurableProgramAnalysis, WrapperCPA, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ThreadModularCPA.class);
  }

  private final ConfigurableProgramAnalysisTM wrappedCpa;
  private final ThreadModularStatistics tStats;

  public ThreadModularCPA(ConfigurableProgramAnalysis pCpa) {
    wrappedCpa = (ConfigurableProgramAnalysisTM) pCpa;
    tStats = new ThreadModularStatistics();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new FlatLatticeDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new ThreadModularTransferRelation(
        wrappedCpa.getTransferRelation(),
        wrappedCpa.getCompatibilityCheck(),
        tStats);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new ThreadModularMergeOperator(
        wrappedCpa.getMergeOperator(),
        wrappedCpa.getMergeForInferenceObject(),
        tStats);
  }

  @Override
  public StopOperator getStopOperator() {
    return new ThreadModularStopOperator(
        wrappedCpa.getStopOperator(),
        wrappedCpa.getStopForInferenceObject(),
        tStats);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new ThreadModularPrecisionAdjustment(wrappedCpa.getPrecisionAdjustment());
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new ThreadModularState(
        new ARGState(wrappedCpa.getInitialState(node, partition), null),
        TauInferenceObject.getInstance());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return wrappedCpa.getInitialPrecision(pNode, pPartition);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(tStats);
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
