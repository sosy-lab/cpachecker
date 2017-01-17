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
package org.sosy_lab.cpachecker.cpa.interval;

import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;

@Options(prefix = "cpa.interval")
public class IntervalAnalysisCPA implements ConfigurableProgramAnalysisWithBAM,
               StatisticsProvider,
               ProofCheckerCPA {

  /**
   * This method returns a CPAfactory for the interval analysis CPA.
   *
   * @return the CPAfactory for the interval analysis CPA
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(IntervalAnalysisCPA.class);
  }

  /**
   * the merge type of the interval analysis
   */
  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN"},
          description="which type of merge operator to use for IntervalAnalysisCPA")
  private String mergeType = "SEP";

  @Option(secure=true, description="at most that many intervals will be tracked per variable, -1 if number not restricted")
  private int threshold = -1;

  private final StateToFormulaWriter writer;
  private final IntervalAbstractDomain abstractDomain;
  private final IntervalAnalysisTransferRelation transferRelation;

  private final LogManager logger;

  /**
   * This method acts as the constructor of the interval analysis CPA.
   *
   * @param config the configuration of the CPAinterval analysis CPA.
   */
  private IntervalAnalysisCPA(
      Configuration config,
      LogManager logger,
      ShutdownNotifier shutdownNotifier,
      CFA cfa)
          throws InvalidConfigurationException {
    config.inject(this);
    abstractDomain = new IntervalAbstractDomain(threshold);
    writer = new StateToFormulaWriter(config, logger, shutdownNotifier, cfa);
    transferRelation = new IntervalAnalysisTransferRelation(
        config, threshold, logger);
    this.logger = logger;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  /* (non-Javadoc)
     * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getMergeOperator()
     */
  @Override
  public MergeOperator getMergeOperator() {
    switch (mergeType.toUpperCase()) {
      case "SEP":
        return MergeSepOperator.getInstance();

      case "JOIN":
        return new MergeJoinOperator(abstractDomain);

      default:
        throw new AssertionError("unknown merge operator");
    }
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(abstractDomain);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public Reducer getReducer() {
    return new IntervalAnalysisReducer();
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getInitialState(org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode)
   */
  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new IntervalAnalysisState();
  }

  @Override
  public Precision getInitialPrecision(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return SingletonPrecision.getInstance();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    writer.collectStatistics(pStatsCollection);
  }
}
