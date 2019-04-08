/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.dca;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.cpa.dca.bfautomaton.BFAutomaton;
import org.sosy_lab.cpachecker.cpa.dca.bfautomaton.BFAutomatonState;

@Options(prefix = "cpa.dca")
public class DCACPA implements StatisticsProvider, ConfigurableProgramAnalysis, ProofCheckerCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DCACPA.class);
  }

  private final Map<BFAutomaton, DCAProperty> automatonMap;

  private final AbstractDomain domain = new FlatLatticeDomain();

  private final DCAStatistics stats = new DCAStatistics(this);
  @SuppressWarnings("unused")
  private final LogManager logger;

  protected DCACPA(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this, DCACPA.class);

    automatonMap = new HashMap<>();
    logger = pLogger;
  }

  ImmutableMap<BFAutomaton, DCAProperty> getAutomatonMap() {
    return ImmutableMap.copyOf(automatonMap);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    if (automatonMap.isEmpty()) {
      return DCAState.EMPTY_STATE;
    }

    ImmutableSet<BFAutomatonState> compositeInitStates =
        automatonMap.keySet()
            .stream()
            .map(BFAutomaton::getInitialState)
            .collect(ImmutableSet.toImmutableSet());
    return DCAState.createInitialState(compositeInitStates, automatonMap.values());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new DCATransferRelation(this);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

}
