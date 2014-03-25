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
package org.sosy_lab.cpachecker.cpa.statistics;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
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
import org.sosy_lab.cpachecker.cpa.statistics.StatisticsState.StatisticsStateFactory;
import org.sosy_lab.cpachecker.cpa.statistics.StatisticsState.StatisticsStateFactory.FactoryAnalysisType;
import org.sosy_lab.cpachecker.cpa.statistics.provider.SimpleIntProviderFactory;
import org.sosy_lab.cpachecker.cpa.statistics.provider.SimpleIntProviderFactory.MergeOption;

/**
 * Implementation of the StatisticsCPA for code analysis within the CPAchecker framework.
 * You can enable the analysis with cpa=cpa.statistics.StatisticsCPA.
 * All metrics can be enabled and disabled on an individual basus with the "cpa.statistics.metric.*" options.
 */
@Options(prefix="cpa.statistics")
public class StatisticsCPA implements StatisticsProvider, ConfigurableProgramAnalysis {

  private StatisticsStateFactory factory;
  private StatisticsCPAStatistics stats;

  @Option(name="metric.nodeCount",
      description="count the number of traversed nodes.")
  private boolean nodeCount = true;

  @Option(name="metric.gotoCount",
      description="count the number of traversed gotos.")
  private boolean gotoCount = true;

  @Option(name="metric.assumeCount",
      description="count the number of traversed assume statements.")
  private boolean assumeCount = true;

  @Option(name="metric.loopCount",
      description="count the number of traversed loops.")
  private boolean loopCount = true;
  @Option(name="metric.functionCallCount",
      description="count the number of traversed function calls.")
  private boolean functionCallCount = true;
  @Option(name="metric.branchCount",
      description="count the number of traversed edges with more then one outgoing edge.")
  private boolean branchCount = true;
  @Option(name="metric.jumpCount",
      description="count the number of traversed jumps.")
  private boolean jumpCount = true;
  @Option(name="metric.functionDefCount",
      description="count the number of traversed function definitions.")
  private boolean functionDefCount = true;
  @Option(name="metric.localVariablesCount",
      description="count the number of traversed local variable definitions.")
  private boolean localVariablesCount = true;
  @Option(name="metric.globalVariablesCount",
      description="count the number of traversed global variable definitions.")
  private boolean globalVariablesCount = true;
  @Option(name="metric.structVariablesCount",
      description="count the number of traversed variable definitions with a complex structure type.")
  private boolean structVariablesCount = true;
  @Option(name="metric.pointerVariablesCount",
      description="count the number of traversed variable definitions with pointer type.")
  private boolean pointerVariablesCount = true;
  @Option(name="metric.arrayVariablesCount",
      description="count the number of traversed variable definitions with array type.")
  private boolean arrayVariablesCount = true;

  @Option(name="metric.bitwiseOperationCount",
      description="count the number of traversed bitwise operations.")
  private boolean bitwiseOperationCount = true;

  @Option(name="metric.arithmeticOperationCount",
      description="count the number of traversed arithmetic operations.")
  private boolean arithmeticOperationCount = true;

  @Option(name="metric.integerVariablesCount",
      description="count the number of traversed variable definitions with integer type.")
  private boolean integerVariablesCount = true;

  @Option(name="metric.floatVariablesCount",
      description="count the number of traversed variable definitions with floating type (float or double).")
  private boolean floatVariablesCount = true;

  @Option(name="metric.dereferenceCount",
      description="count the number of traversed dereference operations.")
  private boolean dereferenceCount = true;

  @Option(name="analysis",
      description="set this to true when you only want to do a code analysis. If StatisticsCPA is combined with other CPAs to do queries use false.")
  private boolean isAnalysis = true;

  @Option(name="mergeSep", values={"sep", "join"},
      description="which merge operator to use for StatisticsCPA? Ignored when analysis is set to true")
  private String mergeType = "sep";


  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;

  public static CPAFactory factory() {
    AutomaticCPAFactory factory = AutomaticCPAFactory.forType(StatisticsCPA.class);
    return factory;
  }

  public StatisticsCPA(Configuration config, LogManager pLogger,
      CFA cfa) throws InvalidConfigurationException {
    config.inject(this);
    FactoryAnalysisType analysisType = FactoryAnalysisType.MetricsQuery;
    if (isAnalysis) {
      analysisType = FactoryAnalysisType.Analysis;
    }

    this.factory = new StatisticsStateFactory(analysisType);
    this.abstractDomain = new StatisticsCPADomain();

    // TODO: refactor this...
    MergeOption defMerge = MergeOption.Add;
    if (nodeCount) {
      factory.addProvider(SimpleIntProviderFactory.getEdgeCountProvider(defMerge));
    }
    if (gotoCount){
      factory.addProvider(SimpleIntProviderFactory.getGotoCountProvider(defMerge));
    }
    if (loopCount){
      factory.addProvider(SimpleIntProviderFactory.getLoopCountProvider(cfa, defMerge));
    }
    if (functionCallCount){
      factory.addProvider(SimpleIntProviderFactory.getFunctionCallCountProvider(defMerge));
    }
    if (branchCount){
      factory.addProvider(SimpleIntProviderFactory.getBranchCountProvider(defMerge));
    }
    if (jumpCount){
      factory.addProvider(SimpleIntProviderFactory.getJumpCountProvider(defMerge));
    }
    if (functionDefCount){
      factory.addProvider(SimpleIntProviderFactory.getFunctionDefCountProvider(defMerge));
    }
    if (localVariablesCount){
      factory.addProvider(SimpleIntProviderFactory.getLocalVariablesCountProvider(defMerge));
    }
    if (globalVariablesCount){
      factory.addProvider(SimpleIntProviderFactory.getGlobalVariablesCountProvider(defMerge));
    }
    if (structVariablesCount){
      factory.addProvider(SimpleIntProviderFactory.getStructVariablesCountProvider(defMerge));
    }
    if (pointerVariablesCount){
      factory.addProvider(SimpleIntProviderFactory.getPointerVariablesCountProvider(defMerge));
    }
    if (arrayVariablesCount){
      factory.addProvider(SimpleIntProviderFactory.getArrayVariablesCountProvider(defMerge));
    }
    if (bitwiseOperationCount){
      factory.addProvider(SimpleIntProviderFactory.getBitwiseOperationCountProvider(defMerge));
    }
    if (arithmeticOperationCount){
      factory.addProvider(SimpleIntProviderFactory.getArithmeticOperationCountProvider(defMerge));
    }
    if (integerVariablesCount){
      factory.addProvider(SimpleIntProviderFactory.getIntegerVariablesCountProvider(defMerge));
    }
    if (floatVariablesCount){
      factory.addProvider(SimpleIntProviderFactory.getFloatVariablesCountProvider(defMerge));
    }
    if (dereferenceCount){
      factory.addProvider(SimpleIntProviderFactory.getDereferenceCountProvider(defMerge));
    }
    if (assumeCount){
      factory.addProvider(SimpleIntProviderFactory.getAssumeCountProvider(defMerge));
    }

    this.stats = new StatisticsCPAStatistics(config, this);

    MergeOperator mergeOp = null;
    if (isAnalysis || mergeType.equals("sep")) {
      mergeOp = MergeSepOperator.getInstance();
    } else if (mergeType.equals("join")) {
      mergeOp = new MergeJoinOperator(abstractDomain);
    }

    mergeOperator = mergeOp;
    stopOperator = new StopSepOperator(abstractDomain);

    this.transferRelation = new StatisticsTransferRelation(config, pLogger);
  }

  public StatisticsStateFactory getFactory() {
    return factory;
  }

  public boolean isAnalysis() {
    return isAnalysis;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    return factory.createNew(pNode);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }


  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
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
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }
}
