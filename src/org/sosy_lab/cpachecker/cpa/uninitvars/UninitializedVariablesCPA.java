/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.uninitvars;

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
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

@Options(prefix="cpa.uninitvars")
public class UninitializedVariablesCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(UninitializedVariablesCPA.class);
  }

  @Option(description="print warnings during analysis when uninitialized variables are used")
  private String printWarnings = "true";
  @Option(name="merge", values={"sep", "join"},
      description="which merge operator to use for UninitializedVariablesCPA?")
  private String mergeType = "sep";
  @Option(name="stop", values={"sep", "join"},
      description="which stop operator to use for UninitializedVariablesCPA?")
  private String stopType = "sep";
  @Option (description="if enabled checks if states are target states (there exist warning for uninitilized use of variables")
  private boolean checkTarget = false;

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  private final UninitializedVariablesStatistics statistics;

  private UninitializedVariablesCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {

    config.inject(this);

    UninitializedVariablesDomain domain = new UninitializedVariablesDomain();

    MergeOperator mergeOp = null;
    if (mergeType.equals("sep")) {
      mergeOp = MergeSepOperator.getInstance();
    } else if (mergeType.equals("join")) {
      mergeOp = new MergeJoinOperator(domain);
    }

    StopOperator stopOp = null;

    if (stopType.equals("sep")) {
      stopOp = new StopSepOperator(domain);
    } else if (stopType.equals("join")) {
      stopOp = new StopJoinOperator(domain);
    }

    if(checkTarget){
      printWarnings = "true";
    }
    UninitializedVariablesState.init(checkTarget);

    this.abstractDomain = domain;
    this.mergeOperator = mergeOp;
    this.stopOperator = stopOp;
    this.transferRelation = new UninitializedVariablesTransferRelation(printWarnings, logger);
    this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();

    statistics = new UninitializedVariablesStatistics(printWarnings);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    return new UninitializedVariablesState(pNode.getFunctionName());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

}
