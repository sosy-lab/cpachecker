/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.propertyscope;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StopAlwaysOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;

public class PropertyScopeCPA implements StatisticsProvider, ConfigurableProgramAnalysis{

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;
  private final PropertyScopeStatistics statistics;
  private final FlatLatticeDomain abstractDomain = new FlatLatticeDomain();
  private final PropertyScopeTransferRelation transfer;
  private final MergeOperator mergeOperator = new MergeSepOperator();
  private final StopOperator stopOperator = new StopAlwaysOperator();
  private final PropertyScopePrecisionAdjustment propertyScopePrecisionAdjustment;

  protected PropertyScopeCPA(
      Configuration config, LogManager logger,
      CFA pCfa, ShutdownNotifier pShutdownNotifier)
      throws CPAException, InvalidConfigurationException {
    //config.inject(this, PropertyScopeCPA.class);
    this.config = config;
    this.logger = logger;
    cfa = pCfa;
    shutdownNotifier = pShutdownNotifier;
    statistics = new PropertyScopeStatistics(config, logger, pCfa);
    transfer = new PropertyScopeTransferRelation(logger);
    propertyScopePrecisionAdjustment = new PropertyScopePrecisionAdjustment(pCfa, logger);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
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
    return propertyScopePrecisionAdjustment;
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PropertyScopeCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return PropertyScopeState.initial(node);

  }

  @Override
  public Precision getInitialPrecision(CFANode node, StateSpacePartition partition) {
    return SingletonPrecision.getInstance();

  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }
}
