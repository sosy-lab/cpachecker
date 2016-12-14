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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;

import java.util.Collection;

@Options(prefix = "cpa.interval")
public class IntervalAnalysisCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider, ProofCheckerCPA {

  /**
   * This method returns a CPAfactory for the interval analysis CPA.
   *
   * @return the CPAfactory for the interval analysis CPA
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(IntervalAnalysisCPA.class);
  }

  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN"},
          description="which type of merge operator to use for IntervalAnalysisCPA")
  /**
   * the merge type of the interval analysis
   */
  private String mergeType = "SEP";

  private final StateToFormulaWriter writer;

  /**
   * This method acts as the constructor of the interval analysis CPA.
   *
   * @param config the configuration of the CPAinterval analysis CPA.
   */
  private IntervalAnalysisCPA(Configuration config, LogManager logger,
      ShutdownNotifier shutdownNotifier, CFA cfa)
          throws InvalidConfigurationException {
    super(
        "irrelevant",
        "sep",
        DelegateAbstractDomain.<IntervalAnalysisState>getInstance(),
        new IntervalAnalysisTransferRelation(config, logger));
    config.inject(this);
    writer = new StateToFormulaWriter(config, logger, shutdownNotifier, cfa);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getMergeOperator()
   */
  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
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
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    writer.collectStatistics(pStatsCollection);
  }
}
