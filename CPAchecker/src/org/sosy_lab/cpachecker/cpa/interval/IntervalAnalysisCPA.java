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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
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
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;

@Options(prefix="cpa.interval")
public class IntervalAnalysisCPA implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider, ProofChecker {

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

  /**
   * the abstract domain of the interval analysis
   */
  private AbstractDomain abstractDomain;

  /**
   * the merge operator of the interval analysis
   */
  private MergeOperator mergeOperator;

  /**
   * the stop operator of the interval analysis
   */
  private StopOperator stopOperator;

  /**
   * the transfer relation of the interval analysis
   */
  private TransferRelation transferRelation;

  /**
   * the precision adjustment of the interval analysis
   */
  private PrecisionAdjustment precisionAdjustment;

  private final IntervalAnalysisReducer reducer;

  private final StateToFormulaWriter writer;

  /**
   * This method acts as the constructor of the interval analysis CPA.
   *
   * @param config the configuration of the CPAinterval analysis CPA.

   * @throws InvalidConfigurationException
   */
  private IntervalAnalysisCPA(Configuration config, LogManager logger,
      ShutdownNotifier shutdownNotifier, CFA cfa)
          throws InvalidConfigurationException {
    config.inject(this);

    abstractDomain      = DelegateAbstractDomain.<IntervalAnalysisState>getInstance();

    mergeOperator       = mergeType.equals("SEP") ? MergeSepOperator.getInstance() : new MergeJoinOperator(abstractDomain);

    stopOperator        = new StopSepOperator(abstractDomain);

    transferRelation    = new IntervalAnalysisTransferRelation(config);

    precisionAdjustment = StaticPrecisionAdjustment.getInstance();

    reducer = new IntervalAnalysisReducer();

    writer = new StateToFormulaWriter(config, logger, shutdownNotifier, cfa);

  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getAbstractDomain()
   */
  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getMergeOperator()
   */
  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getStopOperator()
   */
  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getInitialState(org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode)
   */
  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new IntervalAnalysisState();
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getInitialPrecision(org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode)
   */
  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getPrecisionAdjustment()
   */
  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pState, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    try {
      Collection<? extends AbstractState> computedSuccessors =
          transferRelation.getAbstractSuccessorsForEdge(
              pState, SingletonPrecision.getInstance(), pCfaEdge);
      boolean found;
      for (AbstractState comp:computedSuccessors) {
        found = false;
        for (AbstractState e:pSuccessors) {
          if (isCoveredBy(comp, e)) {
            found = true;
            break;
          }
        }
        if (!found) {
          return false;
        }
      }
    } catch (CPAException e) {
      throw new CPATransferException("Cannot compare abstract successors", e);
    }
    return true;
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState) throws CPAException, InterruptedException {
    return abstractDomain.isLessOrEqual(pState, pOtherState);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    writer.collectStatistics(pStatsCollection);
  }
}
