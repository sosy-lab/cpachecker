/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.ClassInstantiationException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.PartitioningHeuristic;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.predicate.ABMPredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Throwables;


@Options(prefix="cpa.abm")
public class ABMCPA extends AbstractSingleWrapperCPA implements StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ABMCPA.class);
  }

  private BlockPartitioning blockPartitioning;

  private final LogManager logger;
  private final TimedReducer reducer;
  private final ABMTransferRelation transfer;
  private final ABMCPAStatistics stats;
  private final PartitioningHeuristic heuristic;

  @Option(description="Type of partitioning (FunctionAndLoopPartitioning or DelayedFunctionAndLoopPartitioning)\n"
  		              + "or any class that implements a PartitioningHeuristic")
  private String blockHeuristic = "FunctionAndLoopPartitioning";

  private static final String PACKAGE_NAME_PREFIX = "org.sosy_lab.cpachecker.cfa.blocks.builder";

  private <T> T createInstance(String className, Object[] argumentList, Class<T> type) throws CPAException, InvalidConfigurationException {
    Class<?> argumentTypes[] = {LogManager.class};

    try {
      return Classes.createInstance(className, PACKAGE_NAME_PREFIX, argumentTypes, argumentList, type);

    } catch (ClassInstantiationException e) {
      throw new InvalidConfigurationException("Invalid block heuristic specified (" + e.getMessage() + ")!");

    } catch (InvocationTargetException e) {
      Throwable t = e.getCause();
      Throwables.propagateIfPossible(t, CPAException.class, InvalidConfigurationException.class);

      logger.logException(Level.FINE, t, "Unexpected exception during CPA instantiation!");
      throw new CPAException("Unexpected exception " + t.getClass().getSimpleName() + " during CPA instantiation (" + t.getMessage() + ")!");
    }
  }

  public ABMCPA(ConfigurableProgramAnalysis pCpa, Configuration config, LogManager pLogger, ReachedSetFactory pReachedSetFactory) throws InvalidConfigurationException, CPAException {
    super(pCpa);
    config.inject(this);

    logger = pLogger;

    if (!(pCpa instanceof ConfigurableProgramAnalysisWithABM)) {
      throw new InvalidConfigurationException("ABM needs CPAs that are capable for ABM");
    }
    Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithABM)pCpa).getReducer();
    if (wrappedReducer == null) {
      throw new InvalidConfigurationException("ABM needs CPAs that are capable for ABM");
    }
    reducer = new TimedReducer(wrappedReducer);
    transfer = new ABMTransferRelation(config, logger, this, pReachedSetFactory);

    stats = new ABMCPAStatistics(this);
    heuristic = getPartitioningHeuristic();
  }

  @Override
  public AbstractElement getInitialElement(CFANode node)  {
    if (blockPartitioning == null) {
      blockPartitioning = heuristic.buildPartitioning(node);
      transfer.setBlockPartitioning(blockPartitioning);
      ((AbstractSingleWrapperCPA) getWrappedCpa()).retrieveWrappedCpa(ABMPredicateCPA.class).getTransferRelation().setPartitioning(blockPartitioning);
    }
    return getWrappedCpa().getInitialElement(node);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return getWrappedCpa().getInitialPrecision(pNode);
  }

  private PartitioningHeuristic getPartitioningHeuristic() throws CPAException, InvalidConfigurationException {
    return createInstance(blockHeuristic, new Object[]{logger}, PartitioningHeuristic.class);
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
  public PrecisionAdjustment getPrecisionAdjustment() {
    return getWrappedCpa().getPrecisionAdjustment();
  }

  @Override
  public ABMTransferRelation getTransferRelation() {
    return transfer;
  }

  TimedReducer getReducer() {
    return reducer;
  }

  public BlockPartitioning getBlockPartitioning() {
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
