// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import java.nio.file.Path;
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
import org.sosy_lab.cpachecker.cfa.blocks.builder.FunctionAndLoopPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.PartitioningHeuristic;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.bam.TimedReducer.ReducerStatistics;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "cpa.bam")
public abstract class AbstractBAMCPA extends AbstractSingleWrapperCPA {

  @Option(
      secure = true,
      description =
          "Type of partitioning (FunctionAndLoopPartitioning or"
              + " DelayedFunctionAndLoopPartitioning)\n"
              + "or any class that implements a PartitioningHeuristic")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.cfa.blocks.builder")
  private PartitioningHeuristic.Factory blockHeuristic = FunctionAndLoopPartitioning::new;

  @Option(secure = true, description = "export blocks")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportBlocksPath = Path.of("block_cfa.dot");

  @Option(
      secure = true,
      description =
          "This flag determines which precisions should be updated during refinement. We can choose"
              + " between the minimum number of states and all states that are necessary to"
              + " re-explore the program along the error-path.")
  private boolean doPrecisionRefinementForAllStates = false;

  @Option(
      secure = true,
      description =
          "Heuristic: This flag determines which precisions should be updated during "
              + "refinement. This flag also updates the precision of the most inner block.")
  private boolean doPrecisionRefinementForMostInnerBlock = true;

  @Option(
      secure = true,
      description =
          "In some cases BAM cache can not be easily applied. If the option is enabled CPAs can"
              + " inform BAM that the result states should not be used even if there will a cache"
              + " hit.")
  private boolean useDynamicAdjustment = false;

  @Option(
      secure = true,
      description =
          "This flag determines which refinement procedure we should use. "
              + "We can choose between an in-place refinement and a copy-on-write refinement.")
  private boolean useCopyOnWriteRefinement = false;

  @Option(
      secure = true,
      description =
          "By default, the CPA algorithm terminates when finding the first target state, "
              + "which makes it easy to identify this last state. For special analyses, "
              + "we need to search for more target states in the reached-set, "
              + "when reaching a block-exit. This flag is needed if the option "
              + "'cpa.automaton.breakOnTargetState' is unequal to 1.")
  private boolean searchTargetStatesOnExit = false;

  final Timer blockPartitioningTimer = new Timer();
  final ReducerStatistics reducerStatistics;

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  protected final BlockPartitioning blockPartitioning;
  private final BAMCPAStatistics stats;
  private final BAMARGStatistics argStats;
  private final BAMReachedSetExporter exporter;

  protected AbstractBAMCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException, CPAException {
    super(pCpa);
    pConfig.inject(this, AbstractBAMCPA.class);

    if (!(pCpa instanceof ConfigurableProgramAnalysisWithBAM)) {
      throw new InvalidConfigurationException("BAM needs CPAs that are capable for BAM");
    }

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    blockPartitioningTimer.start();
    blockPartitioning = buildBlockPartitioning(pCfa, pConfig);
    blockPartitioningTimer.stop();

    argStats = new BAMARGStatistics(pConfig, pLogger, this, pCpa, pSpecification, pCfa);
    exporter = new BAMReachedSetExporter(pConfig, pLogger, this);
    stats = new BAMCPAStatistics(pConfig, pLogger, this);

    reducerStatistics = new TimedReducer.ReducerStatistics();

    // create a reducer to throw exceptions directly, actually useless code
    getWrappedCpa().getReducer();
  }

  private BlockPartitioning buildBlockPartitioning(CFA pCfa, Configuration pConfig)
      throws InvalidConfigurationException, CPAException {
    final BlockPartitioningBuilder blockBuilder = new BlockPartitioningBuilder();
    PartitioningHeuristic heuristic = blockHeuristic.create(logger, pCfa, pConfig);
    BlockPartitioning partitioning = heuristic.buildPartitioning(blockBuilder);
    if (exportBlocksPath != null) {
      BlockToDotWriter writer = new BlockToDotWriter(partitioning);
      writer.dump(exportBlocksPath, logger);
    }
    getWrappedCpa().setPartitioning(partitioning);
    return partitioning;
  }

  @Override
  protected ConfigurableProgramAnalysisWithBAM getWrappedCpa() {
    // override for visibility
    return (ConfigurableProgramAnalysisWithBAM) super.getWrappedCpa();
  }

  public BlockPartitioning getBlockPartitioning() {
    return Preconditions.checkNotNull(blockPartitioning);
  }

  LogManager getLogger() {
    return logger;
  }

  TimedReducer getReducer() {
    try {
      return new TimedReducer(reducerStatistics, getWrappedCpa().getReducer());
    } catch (InvalidConfigurationException e) {
      // exception would already appear before, see constructor above
      throw new AssertionError(e);
    }
  }

  @Override
  public BAMMergeOperator getMergeOperator() {
    return new BAMMergeOperator(getWrappedCpa().getMergeOperator());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    assert !Iterables.any(pStatsCollection, Predicates.instanceOf(ARGStatistics.class))
        : "exporting ARGs should only be done at this place, when using BAM.";
    pStatsCollection.add(stats);
    pStatsCollection.add(argStats);
    pStatsCollection.add(exporter);
    pStatsCollection.add(getData().getCache());
    super.collectStatistics(pStatsCollection);
  }

  BAMCPAStatistics getStatistics() {
    return stats;
  }

  /** only public for statistics */
  public abstract BAMDataManager getData();

  boolean doPrecisionRefinementForAllStates() {
    return doPrecisionRefinementForAllStates;
  }

  boolean doPrecisionRefinementForMostInnerBlock() {
    return doPrecisionRefinementForMostInnerBlock;
  }

  boolean useCopyOnWriteRefinement() {
    return useCopyOnWriteRefinement;
  }

  boolean useDynamicAdjustment() {
    return useDynamicAdjustment;
  }

  public boolean searchTargetStatesOnExit() {
    return searchTargetStatesOnExit;
  }
}
