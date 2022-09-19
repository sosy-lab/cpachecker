// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.waitlist.AutomatonFailedMatchesWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.AutomatonMatchesWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.BlockConfiguration;
import org.sosy_lab.cpachecker.core.waitlist.BlockWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.BranchBasedWeightedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.CallstackSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.DepthBasedWeightedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.ExplicitSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.LoopIterationSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.LoopstackSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.PostorderSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.ReversePostorderSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.SMGSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.ThreadingSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.core.waitlist.WeightedRandomWaitlist;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariableWaitlist;
import org.sosy_lab.cpachecker.cpa.usage.UsageReachedSet;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageConfiguration;

@Options(prefix = "analysis")
public class ReachedSetFactory {

  private enum ReachedSetType {
    NORMAL,
    LOCATIONMAPPED,
    PARTITIONED,
    PSEUDOPARTITIONED,
    USAGE
  }

  @Option(
      secure = true,
      name = "traversal.order",
      description = "which strategy to adopt for visiting states?")
  private Waitlist.TraversalMethod traversalMethod = Waitlist.TraversalMethod.DFS;

  @Option(
      secure = true,
      name = "traversal.useCallstack",
      description =
          "handle states with a deeper callstack first"
              + "\nThis needs the CallstackCPA instance to have any effect.")
  private boolean useCallstack = false;

  @Option(
      secure = true,
      name = "traversal.useLoopIterationCount",
      description = "handle states with more loop iterations first.")
  private boolean useLoopIterationCount = false;

  @Option(
      secure = true,
      name = "traversal.useReverseLoopIterationCount",
      description = "handle states with fewer loop iterations first.")
  private boolean useReverseLoopIterationCount = false;

  @Option(
      secure = true,
      name = "traversal.useLoopstack",
      description = "handle states with a deeper loopstack first.")
  private boolean useLoopstack = false;

  @Option(
      secure = true,
      name = "traversal.useReverseLoopstack",
      description = "handle states with a more shallow loopstack first.")
  private boolean useReverseLoopstack = false;

  @Option(
      secure = true,
      name = "traversal.useReversePostorder",
      description =
          "Use an implementation of reverse postorder strategy that allows to select a secondary"
              + " strategy that is used if there are two states with the same reverse postorder id."
              + " The secondary strategy is selected with 'analysis.traversal.order'.")
  private boolean useReversePostorder = false;

  @Option(
      secure = true,
      name = "traversal.usePostorder",
      description =
          "Use an implementation of postorder strategy that allows to select a secondary strategy"
              + " that is used if there are two states with the same postorder id. The secondary"
              + " strategy is selected with 'analysis.traversal.order'.")
  private boolean usePostorder = false;

  @Option(
      secure = true,
      name = "traversal.useExplicitInformation",
      description =
          "handle more abstract states (with less information) first? (only for ExplicitCPA)")
  private boolean useExplicitInformation = false;

  @Option(
      secure = true,
      name = "traversal.useAutomatonInformation",
      description =
          "handle abstract states with more automaton matches first? (only if AutomatonCPA"
              + " enabled)")
  private boolean useAutomatonInformation = false;

  @Option(
      secure = true,
      name = "traversal.byAutomatonVariable",
      description = "traverse in the order defined by the values of an automaton variable")
  private @Nullable String byAutomatonVariable = null;

  @Option(
      secure = true,
      name = "traversal.useNumberOfThreads",
      description = "handle abstract states with fewer running threads first? (needs ThreadingCPA)")
  private boolean useNumberOfThreads = false;

  @Option(
      secure = true,
      name = "traversal.useNumberOfHeapObjects",
      description = "handle abstract states with fewer heap objects first? (needs SMGCPA)")
  private boolean useNumberOfHeapObjects = false;

  @Option(
      secure = true,
      name = "traversal.weightedDepth",
      description = "perform a weighted random selection based on the depth in the ARG")
  private boolean useWeightedDepthOrder = false;

  @Option(
      secure = true,
      name = "traversal.weightedBranches",
      description = "perform a weighted random selection based on the branching depth")
  private boolean useWeightedBranchOrder = false;

  @Option(
      secure = true,
      name = "traversal.useBlocks",
      description =
          "use blocks and set resource limits for its traversal, blocks are handled in DFS order")
  private boolean useBlocks = false;

  @Option(
      secure = true,
      name = "reachedSet",
      description =
          "which reached set implementation to use?\n"
              + "NORMAL: just a simple set\n"
              + "LOCATIONMAPPED: a different set per location (faster, states with different"
              + " locations cannot be merged)\n"
              + "PARTITIONED: partitioning depending on CPAs (e.g Location, Callstack etc.)\n"
              + "PSEUDOPARTITIONED: based on PARTITIONED, uses additional info about the states'"
              + " lattice (maybe faster for some special analyses which use merge_sep and stop_sep")
  private ReachedSetType reachedSet = ReachedSetType.PARTITIONED;

  @Option(
      secure = true,
      name = "reachedSet.withStatistics",
      description = "track more statistics about the reachedset")
  private boolean withStatistics = false;

  private @Nullable BlockConfiguration blockConfig;
  private @Nullable UsageConfiguration usageConfig;
  private WeightedRandomWaitlist.@Nullable WaitlistOptions weightedWaitlistOptions;
  private final LogManager logger;

  public ReachedSetFactory(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = checkNotNull(pLogger);

    if (useBlocks) {
      blockConfig = new BlockConfiguration(pConfig);
    } else {
      blockConfig = null;
    }
    if (reachedSet == ReachedSetType.USAGE) {
      usageConfig = new UsageConfiguration(pConfig);
    } else {
      usageConfig = null;
    }
    if (useWeightedDepthOrder || useWeightedBranchOrder) {
      weightedWaitlistOptions = new WeightedRandomWaitlist.WaitlistOptions(pConfig);
    } else {
      weightedWaitlistOptions = null;
    }
  }

  /**
   * Creates an instance of a {@link ReachedSet}.
   *
   * @param cpa The CPA whose abstract states will be stored in this reached set.
   */
  public ReachedSet create(ConfigurableProgramAnalysis cpa) {
    checkNotNull(cpa);
    WaitlistFactory waitlistFactory = traversalMethod;

    if (useWeightedDepthOrder) {
      waitlistFactory =
          DepthBasedWeightedWaitlist.factory(waitlistFactory, weightedWaitlistOptions);
    }

    if (useWeightedBranchOrder) {
      waitlistFactory =
          BranchBasedWeightedWaitlist.factory(waitlistFactory, weightedWaitlistOptions);
    }

    if (useAutomatonInformation) {
      waitlistFactory = AutomatonMatchesWaitlist.factory(waitlistFactory);
      waitlistFactory = AutomatonFailedMatchesWaitlist.factory(waitlistFactory);
    }
    if (useReversePostorder) {
      waitlistFactory = ReversePostorderSortedWaitlist.factory(waitlistFactory);
    }
    if (usePostorder) {
      waitlistFactory = PostorderSortedWaitlist.factory(waitlistFactory);
    }
    if (useLoopIterationCount) {
      waitlistFactory = LoopIterationSortedWaitlist.factory(waitlistFactory);
    }
    if (useReverseLoopIterationCount) {
      waitlistFactory = LoopIterationSortedWaitlist.reversedFactory(waitlistFactory);
    }
    if (useLoopstack) {
      waitlistFactory = LoopstackSortedWaitlist.factory(waitlistFactory);
    }
    if (useReverseLoopstack) {
      waitlistFactory = LoopstackSortedWaitlist.reversedFactory(waitlistFactory);
    }
    if (useCallstack) {
      waitlistFactory = CallstackSortedWaitlist.factory(waitlistFactory);
    }
    if (useExplicitInformation) {
      waitlistFactory = ExplicitSortedWaitlist.factory(waitlistFactory);
    }
    if (byAutomatonVariable != null) {
      waitlistFactory = AutomatonVariableWaitlist.factory(waitlistFactory, byAutomatonVariable);
    }
    if (useNumberOfThreads) {
      waitlistFactory = ThreadingSortedWaitlist.factory(waitlistFactory);
    }
    if (useNumberOfHeapObjects) {
      waitlistFactory = SMGSortedWaitlist.factory(waitlistFactory);
    }
    if (useBlocks) {
      waitlistFactory = BlockWaitlist.factory(waitlistFactory, blockConfig, logger);
    }

    ReachedSet reached;
    switch (reachedSet) {
      case PARTITIONED:
        reached = new PartitionedReachedSet(cpa, waitlistFactory);
        break;
      case PSEUDOPARTITIONED:
        reached = new PseudoPartitionedReachedSet(cpa, waitlistFactory);
        break;
      case LOCATIONMAPPED:
        reached = new LocationMappedReachedSet(cpa, waitlistFactory);
        break;
      case USAGE:
        reached = new UsageReachedSet(cpa, waitlistFactory, usageConfig, logger);
        break;
      case NORMAL:
      default:
        reached = new DefaultReachedSet(cpa, waitlistFactory);
    }

    if (withStatistics) {
      reached = new StatisticsReachedSet(reached);
    }

    return reached;
  }

  /**
   * Create a new reached set like in {@link #create} and add an initial abstract state from the
   * CPA.
   */
  public ReachedSet createAndInitialize(
      ConfigurableProgramAnalysis cpa, CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    checkNotNull(node);
    checkNotNull(partition);
    ReachedSet reached = create(cpa);
    reached.add(cpa.getInitialState(node, partition), cpa.getInitialPrecision(node, partition));
    return reached;
  }
}
