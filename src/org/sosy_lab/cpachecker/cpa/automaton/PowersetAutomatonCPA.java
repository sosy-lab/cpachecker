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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory.OptionalAnnotation;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
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

import com.google.common.collect.ImmutableSet;

/**
 * Represents one automaton.
 *  One abstract state consists of a set of different automata states (power set).
 */
@Options(prefix="cpa.automaton")
public class PowersetAutomatonCPA implements ConfigurableProgramAnalysis, StatisticsProvider, ConfigurableProgramAnalysisWithBAM {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PowersetAutomatonCPA.class);
  }

  private final ControlAutomatonCPA automatonCpa;
  private final PowersetAutomatonDomain domain;
  private final PowersetAutomatonTransferRelation transfer;
  private final PowersetAutomatonPrecisionAdjustent prec;
  private final MergeOperator merge;
  private final StopSepOperator stop;

  public PowersetAutomatonCPA(@OptionalAnnotation Automaton pAutomaton,
      Configuration pConfig, LogManager pLogger, CFA pCFA)
    throws InvalidConfigurationException {

    automatonCpa = new ControlAutomatonCPA(pAutomaton, pConfig, pLogger, pCFA);
    domain = new PowersetAutomatonDomain(PowersetAutomatonState.TOP);
    transfer = new PowersetAutomatonTransferRelation(automatonCpa.getTransferRelation(), pConfig);
    prec = new PowersetAutomatonPrecisionAdjustent(automatonCpa.getPrecisionAdjustment());
    merge = new MergeJoinOperator(domain);
    stop = new StopSepOperator(domain);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    automatonCpa.collectStatistics(pStatsCollection);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }

  @Override
  public PowersetAutomatonState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new PowersetAutomatonState(
        ImmutableSet.<AutomatonState>of(
            automatonCpa.getInitialState(pNode, pPartition)));
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return automatonCpa.getInitialPrecision(pNode, pPartition);
  }

  @Override
  public Reducer getReducer() {
    return NoOpReducer.getInstance();
  }

}
