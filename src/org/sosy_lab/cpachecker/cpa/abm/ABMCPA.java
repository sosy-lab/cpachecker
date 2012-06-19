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
package org.sosy_lab.cpachecker.cpa.abm;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.FunctionAndLoopPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.PartitioningHeuristic;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.predicate.ABMPredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;


@Options(prefix="cpa.abm")
public class ABMCPA extends AbstractSingleWrapperCPA implements StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ABMCPA.class);
  }

  private BlockPartitioning blockPartitioning;

  private final LogManager logger;
  private final TimedReducer reducer;
  private final ABMTransferRelation transfer;
  private final ABMPrecisionAdjustment prec;
  private final ABMCPAStatistics stats;
  private final PartitioningHeuristic heuristic;
  private final CFA cfa;

  @Option(description="Type of partitioning (FunctionAndLoopPartitioning or DelayedFunctionAndLoopPartitioning)\n"
                    + "or any class that implements a PartitioningHeuristic")
  @ClassOption(packagePrefix="org.sosy_lab.cpachecker.cfa.blocks.builder")
  private Class<? extends PartitioningHeuristic> blockHeuristic = FunctionAndLoopPartitioning.class;

  public ABMCPA(ConfigurableProgramAnalysis pCpa, Configuration config, LogManager pLogger, ReachedSetFactory pReachedSetFactory, CFA pCfa) throws InvalidConfigurationException, CPAException {
    super(pCpa);
    config.inject(this);

    logger = pLogger;
    cfa = pCfa;

    if (!(pCpa instanceof ConfigurableProgramAnalysisWithABM)) {
      throw new InvalidConfigurationException("ABM needs CPAs that are capable for ABM");
    }
    Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithABM)pCpa).getReducer();
    if (wrappedReducer == null) {
      throw new InvalidConfigurationException("ABM needs CPAs that are capable for ABM");
    }
    reducer = new TimedReducer(wrappedReducer);
    prec = new ABMPrecisionAdjustment(getWrappedCpa().getPrecisionAdjustment());
    transfer = new ABMTransferRelation(config, logger, this, pReachedSetFactory);

    stats = new ABMCPAStatistics(this);
    heuristic = getPartitioningHeuristic();
  }

  @Override
  public AbstractState getInitialState(CFANode node)  {
    if (blockPartitioning == null) {
      blockPartitioning = heuristic.buildPartitioning(node);
      transfer.setBlockPartitioning(blockPartitioning);

      ABMPredicateCPA predicateCpa = ((WrapperCPA) getWrappedCpa()).retrieveWrappedCpa(ABMPredicateCPA.class);
      if(predicateCpa != null) {
        predicateCpa.setPartitioning(blockPartitioning);
      }

      Map<AbstractState, Precision> forwardPrecisionToExpandedPrecision = new HashMap<AbstractState, Precision>();
      transfer.setForwardPrecisionToExpandedPrecision(forwardPrecisionToExpandedPrecision);
      prec.setForwardPrecisionToExpandedPrecision(forwardPrecisionToExpandedPrecision);
    }
    return getWrappedCpa().getInitialState(node);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return getWrappedCpa().getInitialPrecision(pNode);
  }

  private PartitioningHeuristic getPartitioningHeuristic() throws CPAException, InvalidConfigurationException {
    return Classes.createInstance(PartitioningHeuristic.class, blockHeuristic, new Class[]{LogManager.class, CFA.class}, new Object[]{logger, cfa}, CPAException.class);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return getWrappedCpa().getAbstractDomain();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return getWrappedCpa().getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return getWrappedCpa().getStopOperator();
  }

  @Override
  public ABMPrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }

  @Override
  public ABMTransferRelation getTransferRelation() {
    return transfer;
  }

  TimedReducer getReducer() {
    return reducer;
  }

  @Override
  protected ConfigurableProgramAnalysis getWrappedCpa() {
    // override for visibility
    return super.getWrappedCpa();
  }

  BlockPartitioning getBlockPartitioning() {
    checkState(blockPartitioning != null);
    return blockPartitioning;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }

  ABMCPAStatistics getStatistics() {
    return stats;
  }
}
