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
package org.sosy_lab.cpachecker.cpa.bam;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.BlockToDotWriter;
import org.sosy_lab.cpachecker.cfa.blocks.builder.FunctionAndLoopPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.PartitioningHeuristic;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Preconditions;


@Options(prefix = "cpa.bam")
public class BAMCPA extends AbstractSingleWrapperCPA implements StatisticsProvider, ProofChecker {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BAMCPA.class);
  }

  private BlockPartitioning blockPartitioning;

  private final LogManager logger;
  private final TimedReducer reducer;
  private final BAMTransferRelation transfer;
  private final BAMPrecisionAdjustment prec;
  private final BAMMergeOperator merge;
  private final BAMStopOperator stop;
  private final BAMCPAStatistics stats;
  private final PartitioningHeuristic heuristic;
  private final CFA cfa;
  private final ProofChecker wrappedProofChecker;

  @Option(description = "Type of partitioning (FunctionAndLoopPartitioning or DelayedFunctionAndLoopPartitioning)\n"
      + "or any class that implements a PartitioningHeuristic")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.cfa.blocks.builder")
  private Class<? extends PartitioningHeuristic> blockHeuristic = FunctionAndLoopPartitioning.class;

  @Option(description = "export blocks")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportBlocksPath = Paths.get("block_cfa.dot");

  public BAMCPA(ConfigurableProgramAnalysis pCpa, Configuration config, LogManager pLogger,
      ReachedSetFactory pReachedSetFactory, ShutdownNotifier pShutdownNotifier, CFA pCfa) throws InvalidConfigurationException, CPAException {
    super(pCpa);
    config.inject(this);

    logger = pLogger;
    cfa = pCfa;

    if (!(pCpa instanceof ConfigurableProgramAnalysisWithBAM)) { throw new InvalidConfigurationException(
        "BAM needs CPAs that are capable for BAM"); }
    Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithBAM) pCpa).getReducer();
    if (wrappedReducer == null) { throw new InvalidConfigurationException("BAM needs CPAs that are capable for BAM"); }

    if (pCpa instanceof ProofChecker) {
      this.wrappedProofChecker = (ProofChecker) pCpa;
    } else {
      this.wrappedProofChecker = null;
    }
    reducer = new TimedReducer(wrappedReducer);
    final BAMCache cache = new BAMCache(config, reducer);
    transfer = new BAMTransferRelation(config, logger, this, wrappedProofChecker, cache, pReachedSetFactory, pShutdownNotifier);
    prec = new BAMPrecisionAdjustment(pCpa.getPrecisionAdjustment(), transfer);
    merge = new BAMMergeOperator(pCpa.getMergeOperator(), transfer);
    stop = new BAMStopOperator(pCpa.getStopOperator());

    stats = new BAMCPAStatistics(this, cache);
    heuristic = getPartitioningHeuristic();
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    if (blockPartitioning == null) {
      blockPartitioning = heuristic.buildPartitioning(node);

      if (exportBlocksPath != null) {
        BlockToDotWriter writer = new BlockToDotWriter(blockPartitioning);
        writer.dump(exportBlocksPath, logger);
      }

      transfer.setBlockPartitioning(blockPartitioning);

      BAMPredicateCPA predicateCpa = ((WrapperCPA) getWrappedCpa()).retrieveWrappedCpa(BAMPredicateCPA.class);
      if (predicateCpa != null) {
        predicateCpa.setPartitioning(blockPartitioning);
      }

      Map<AbstractState, Precision> forwardPrecisionToExpandedPrecision = new HashMap<>();
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
    return Classes.createInstance(PartitioningHeuristic.class, blockHeuristic, new Class[] { LogManager.class,
        CFA.class }, new Object[] { logger, cfa }, CPAException.class);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return getWrappedCpa().getAbstractDomain();
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
  public BAMPrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }

  @Override
  public BAMTransferRelation getTransferRelation() {
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

  BAMCPAStatistics getStatistics() {
    return stats;
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pState, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return transfer.areAbstractSuccessors(pState, pCfaEdge, pSuccessors);
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState) throws CPAException, InterruptedException {
    Preconditions.checkNotNull(wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return wrappedProofChecker.isCoveredBy(pState, pOtherState);
  }
}
