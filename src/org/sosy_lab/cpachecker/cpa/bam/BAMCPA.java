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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.BlockToDotWriter;
import org.sosy_lab.cpachecker.cfa.blocks.builder.BlockPartitioningBuilder;
import org.sosy_lab.cpachecker.cfa.blocks.builder.ExtendedBlockPartitioningBuilder;
import org.sosy_lab.cpachecker.cfa.blocks.builder.FunctionAndLoopPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.PartitioningHeuristic;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;


@Options(prefix = "cpa.bam")
public class BAMCPA extends AbstractSingleWrapperCPA implements StatisticsProvider, ProofChecker {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BAMCPA.class);
  }

  private final BlockPartitioning blockPartitioning;

  private final LogManager logger;
  private final TimedReducer reducer;
  private final BAMTransferRelation transfer;
  private final BAMCPAStatistics stats;
  private final BAMARGStatistics argStats;
  private final PartitioningHeuristic heuristic;
  private final ProofChecker wrappedProofChecker;
  private final BAMDataManager data;
  private final BAMPCCManager bamPccManager;

  final Timer blockPartitioningTimer = new Timer();

  @Option(
    secure = true,
    description =
        "Type of partitioning (FunctionAndLoopPartitioning or DelayedFunctionAndLoopPartitioning)\n"
            + "or any class that implements a PartitioningHeuristic"
  )
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.cfa.blocks.builder")
  private PartitioningHeuristic.Factory blockHeuristic = FunctionAndLoopPartitioning::new;

  @Option(secure=true, description = "export blocks")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportBlocksPath = Paths.get("block_cfa.dot");

  @Option(name = "handleRecursiveProcedures", secure = true,
      description = "BAM allows to analyse recursive procedures. This strongly depends on the underlying CPA. "
          + "The current support includes only ValueAnalysis and PredicateAnalysis (with tree interpolation enabled).")
  private boolean handleRecursiveProcedures = false;

  @Option(secure = true,
      description = "This flag determines which precisions should be updated during refinement. "
      + "We can choose between the minimum number of states and all states that are necessary "
      + "to re-explore the program along the error-path.")
  private boolean doPrecisionRefinementForAllStates = false;

  @Option(secure = true,
      description = "Use more fast partitioning builder, which can not handle loops")
  private boolean useExtendedPartitioningBuilder = false;


  public BAMCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException, CPAException {
    super(pCpa);
    config.inject(this);

    logger = pLogger;

    if (!(pCpa instanceof ConfigurableProgramAnalysisWithBAM)) {
      throw new InvalidConfigurationException("BAM needs CPAs that are capable for BAM");
    }

    if (pCpa instanceof ProofChecker) {
      this.wrappedProofChecker = (ProofChecker) pCpa;
    } else {
      this.wrappedProofChecker = null;
    }

    Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithBAM) pCpa).getReducer();
    reducer = new TimedReducer(wrappedReducer);

    final BAMCache cache = new BAMCacheImpl(config, reducer, logger);
    data = new BAMDataManager(cache, pReachedSetFactory, pLogger);

    heuristic = blockHeuristic.create(pLogger, pCfa, config);

    blockPartitioningTimer.start();
    blockPartitioning = buildBlockPartitioning(pCfa);
    blockPartitioningTimer.stop();

    bamPccManager = new BAMPCCManager(
        wrappedProofChecker,
        config,
        blockPartitioning,
        wrappedReducer,
        this,
        data);

    if (handleRecursiveProcedures) {

      if (pCfa.getVarClassification().isPresent() && !pCfa.getVarClassification().get().getRelevantFields().isEmpty()) {
        // TODO remove this ugly hack as soon as possible :-)
        throw new UnsupportedCCodeException("BAM does not support pointer-analysis for recursive programs.",
            pCfa.getMainFunction().getLeavingEdge(0));
      }

      transfer =
          new BAMTransferRelationWithFixPointForRecursion(
              config,
              this,
              wrappedProofChecker,
              pShutdownNotifier);
    } else {
      transfer =
          new BAMTransferRelation(
              config,
              this,
              wrappedProofChecker,
              pShutdownNotifier);
    }
    stats = new BAMCPAStatistics(this, data, config, logger);
    argStats = new BAMARGStatistics(config, pLogger, this, pCpa, pSpecification, pCfa);
  }

  private BlockPartitioning buildBlockPartitioning(CFA pCfa) {
    BlockPartitioningBuilder blockBuilder;
    if (useExtendedPartitioningBuilder) {
      blockBuilder = new ExtendedBlockPartitioningBuilder();
    } else {
      blockBuilder = new BlockPartitioningBuilder();
    }
    BlockPartitioning partitioning = heuristic.buildPartitioning(pCfa, blockBuilder);

    if (exportBlocksPath != null) {
      BlockToDotWriter writer = new BlockToDotWriter(partitioning);
      writer.dump(exportBlocksPath, logger);
    }
    getWrappedCpa().setPartitioning(partitioning);
    return partitioning;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new BAMMergeOperator(getWrappedCpa().getMergeOperator(), bamPccManager);
  }

  @Override
  public StopOperator getStopOperator() {
    return handleRecursiveProcedures
        ? new BAMStopOperatorForRecursion(getWrappedCpa().getStopOperator())
        : new BAMStopOperator(getWrappedCpa().getStopOperator());
  }

  @Override
  public BAMPrecisionAdjustment getPrecisionAdjustment() {
    return new BAMPrecisionAdjustment(
        getWrappedCpa().getPrecisionAdjustment(), data, bamPccManager,
        logger, blockPartitioning);
  }

  @Override
  public BAMTransferRelation getTransferRelation() {
    return transfer;
  }

  TimedReducer getReducer() {
    return reducer;
  }

  @Override
  protected ConfigurableProgramAnalysisWithBAM getWrappedCpa() {
    // override for visibility
    return (ConfigurableProgramAnalysisWithBAM) super.getWrappedCpa();
  }

  public BlockPartitioning getBlockPartitioning() {
    Preconditions.checkNotNull(blockPartitioning);
    return blockPartitioning;
  }

  BAMDataManager getData() {
    Preconditions.checkNotNull(data);
    return data;
  }

  public BAMPCCManager getBamPccManager() {
    return bamPccManager;
  }

  LogManager getLogger() {
    return logger;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    assert !Iterables.any(pStatsCollection, Predicates.instanceOf(ARGStatistics.class))
        : "exporting ARGs should only be done at this place, when using BAM.";
    pStatsCollection.add(argStats);
    pStatsCollection.add(stats);
    pStatsCollection.add(data.bamCache);
    super.collectStatistics(pStatsCollection);
  }

  BAMCPAStatistics getStatistics() {
    return stats;
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pState, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return bamPccManager.areAbstractSuccessors(pState, pCfaEdge, pSuccessors);
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState) throws CPAException, InterruptedException {
    Preconditions.checkNotNull(wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return wrappedProofChecker.isCoveredBy(pState, pOtherState);
  }

  boolean doPrecisionRefinementForAllStates() {
    return doPrecisionRefinementForAllStates;
  }
}
