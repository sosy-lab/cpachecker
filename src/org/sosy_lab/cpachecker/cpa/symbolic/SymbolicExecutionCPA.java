/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.symbolic;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Configurable Program Analysis using symbolic execution for analysis.
 */
public class SymbolicExecutionCPA implements ConfigurableProgramAnalysis, StatisticsProvider, ProofChecker {

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private VariableTrackingPrecision precision;
  private PrecisionAdjustment precisionAdjustment;


  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SymbolicExecutionCPA.class);
  }

  private SymbolicExecutionCPA(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      CFA pCfa) throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;

    abstractDomain = DelegateAbstractDomain.<ValueAnalysisState>getInstance();
    mergeOperator = initializeMergeOperator();
    stopOperator = initializeStopOperator();
    transferRelation = new SymbolicExecutionTransferRelation(config, logger, cfa);
  }

  private MergeOperator initializeMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  private StopOperator initializeStopOperator() {
    return new StopSepOperator(abstractDomain);
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
    return null;
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    return new SymbolicExecutionState();
  }

  @Override
  public Precision getInitialPrecision(CFANode node) {
    return null;
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState state, CFAEdge cfaEdge,
      Collection<? extends AbstractState> successors) throws CPATransferException, InterruptedException {
    return false;
  }

  @Override
  public boolean isCoveredBy(AbstractState state, AbstractState otherState) throws CPAException, InterruptedException {
    return false;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {

  }
}
