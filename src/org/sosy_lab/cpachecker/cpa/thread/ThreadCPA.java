/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

import com.google.common.base.Preconditions;


public class ThreadCPA implements ConfigurableProgramAnalysis, WrapperCPA, ConfigurableProgramAnalysisWithBAM, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ThreadCPA.class);
  }

  private final AbstractDomain abstractDomain = new FlatLatticeDomain();
  private final MergeOperator mergeOperator = MergeSepOperator.getInstance();
  private final StopOperator stopOperator = new StopSepOperator(abstractDomain);
  private final LocationCPA locationCPA;
  private final CallstackCPA callstackCPA;
  private final ThreadTransferRelation transferRelation;
  private final ThreadReducer reducer;

  public ThreadCPA(Configuration config, LogManager pLogger, CFA pCfa) throws InvalidConfigurationException {
    locationCPA = new LocationCPA(pCfa, config);
    callstackCPA = new CallstackCPA(config, pLogger, pCfa);
    transferRelation = new ThreadTransferRelation(locationCPA.getTransferRelation(), callstackCPA.getTransferRelation(), config);
    reducer = new ThreadReducer(locationCPA.getReducer(), callstackCPA.getReducer());
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    Preconditions.checkNotNull(pNode);
    return ThreadState.emptyState((LocationState)locationCPA.getInitialState(pNode, pPartition),
                                    (CallstackState)callstackCPA.getInitialState(pNode, pPartition));
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition partition) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(CallstackCPA.class)) {
      return pType.cast(callstackCPA);
    } else if (pType.isAssignableFrom(LocationCPA.class)) {
      return pType.cast(locationCPA);
    }
    return null;
  }

  @Override
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs() {
    List<ConfigurableProgramAnalysis> cpas = new ArrayList<>(2);
    cpas.add(locationCPA);
    cpas.add(callstackCPA);
    return cpas;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(transferRelation.getStatistics());
  }
}
