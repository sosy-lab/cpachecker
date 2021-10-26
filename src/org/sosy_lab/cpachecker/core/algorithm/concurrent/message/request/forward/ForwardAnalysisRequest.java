// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.forward;

import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.CPACreatingRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.RequestInvalidatedException;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.TaskRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward.ForwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

@Options(prefix = "concurrent.task.forward")
public class ForwardAnalysisRequest extends CPACreatingRequest implements TaskRequest {
  private final Configuration taskConfiguration;
  private final Block predecessor;
  private final Block block;
  private final int expectedPredVersion;
  private final ShareableBooleanFormula newSummary;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(description = "Configuration file for forward analysis during concurrent analysis.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configFile = null;

  public ForwardAnalysisRequest(
      @Nullable final Block pPredecessor,
      final Block pBlock,
      @Nullable final ShareableBooleanFormula pNewSummary,
      final int pExpectedPredVersion,
      final Configuration pConfig,
      final Specification pSpecification,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCFA,
      final MessageFactory pMessageFactory)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(pMessageFactory, pLogger, pShutdownNotifier);
    
    pConfig.inject(this);
    taskConfiguration = ForwardAnalysis.getConfiguration(pLogger, configFile, pConfig);

    prepareCPA(taskConfiguration, pCFA, pSpecification, pBlock);
    
    predecessor = pPredecessor;
    block = pBlock;
    expectedPredVersion = pExpectedPredVersion;

    PredicateCPA predicateCPA = argcpa.retrieveWrappedCpa(PredicateCPA.class);

    FormulaManagerView formulaManager = predicateCPA.getSolver().getFormulaManager();
    PathFormulaManager pfMgr = predicateCPA.getPathFormulaManager();

    newSummary =
        (pNewSummary == null)
        ? new ShareableBooleanFormula(formulaManager, pfMgr.makeEmptyPathFormula())
        : pNewSummary;
    
    assert algorithm != null && argcpa != null && reached != null;
  }

  /**
   * Increment version count of the block summary for {@code pBlock}.<br>
   * Used by {@link #process(Table, Map, Set)}.
   *
   * @param pBlock The block for which to update the summary version
   * @param pVersions The global map of summary versions
   * @return The new summary version
   * @see #process(Table, Map, Set)
   */
  private static int incrementVersion(final Block pBlock, final Map<Block, Integer> pVersions) {
    assert Thread.currentThread().getName().equals(Scheduler.getThreadName())
        : "Only " + Scheduler.getThreadName() + " may call incrementVersion()";

    int currentVersion = pVersions.getOrDefault(pBlock, 0);
    pVersions.put(pBlock, ++currentVersion);
    return currentVersion;
  }

  /**
   * {@inheritDoc}
   * <hr/>
   *
   * <p>For {@link ForwardAnalysisRequest}, this method performs the following steps:
   *
   * <ol>
   *   <li>Publish updated block summary of predecessor block, if applicable.<br>
   *       In most cases, a new ForwardAnalysis <em>t1</em> on a block <em>b1</em> has been
   *       triggered by a completed ForwardAnalysis <em>t0</em> on a block <em>b0</em>, where
   *       <em>b0</em> belongs to the set of predecessors of <em>b1</em>. This other analysis
   *       <em>t0</em> usually identified a new block summary <em>Q0</em> for <em>b0</em>. Because
   *       the tasks themselves don't gain access to the global synchronization structures, {@link
   *       #process(Table, Map, Set)} running for <em>t1</em> offers the first opportunity after
   *       the completion of <em>t0</em> to access the global map of calculated summaries and
   *       publish <em>Q0</em> to it. Before doing so, {@link #process(Table, Map, Set)} running
   *       for <em>t1</em> first checks the expected version of the summary for <em>b0</em> against
   *       the value found in the global map of version counters. At the time the
   *       {@link ForwardAnalysisRequest} <em>t1</em> got created by <em>t0</em>, <em>t0</em>
   *       provided <em>t1</em> with the version applicable when <em>t0</em> was scheduled. If
   *       {@link #process(Table, Map, Set)} for <em>t1</em> still encounters the same version in
   *       the global map, the updated summary <em>Q0</em> found by <em>t0</em> remains valid and
   *       must be published. If the version has been increment further in the meantime, the
   *       calculation of a newer version for the block summary of <em>b0</em> has been scheduled
   *       with a {@link ForwardAnalysisRequest} <em>t2</em>. Furthermore, <em>t2</em> might even
   *       already have completed, and its result might have been published to the global map of
   *       block summaries. In this case, {@link #process(Table, Map, Set)} for <em>t1</em> must
   *       not overwrite this value with its now outdated version of <em>Q0</em>. Such task
   *       <em>t2</em> would also already have created a new {@link ForwardAnalysisRequest} on
   *       <em>b1</em>, which makes the present task <em>t1</em> redundant. In this case, the method
   *       throws a {@link RequestInvalidatedException} and the {@link ForwardAnalysisRequest} shall be
   *       discarded.
   *   <li>Store the old predecessor summary whose update triggered the creation of the present
   *       task. As soon as the task actually executes, it uses the old value to check whether new
   *       and old formula actually differ. If they do not, it completes early and performs no new
   *       analysis.
   *   <li>Increment the summary version of the block this task operates on by one and store the new
   *       value in the task.
   *   <li>Retrieve the list of all predecessor summaries of the current block and store it in the
   *       task. As soon as the task executes, it uses these summaries to construct the cumulative
   *       predecessor summary.
   * </ol>
   *
   * @return The immutable {@link ForwardAnalysis} which actually implements the {@link Task}.
   * @throws RequestInvalidatedException The {@link ForwardAnalysisRequest} has become invalidated by a
   *                                     more recent one and the {@link ForwardAnalysis} must not execute.
   * @see ForwardAnalysis
   */
  @Override
  public Task process(
      final Table<Block, Block, ShareableBooleanFormula> pSummaries,
      final Map<Block, Integer> pSummaryVersions,
      final Set<CFANode> pAlreadyPropagated)
      throws RequestInvalidatedException {
    assert Thread.currentThread().getName().equals(Scheduler.getThreadName())
        : "Only " + Scheduler.getThreadName() + " may call finalize()";

    final Map<Block, ShareableBooleanFormula> incoming = pSummaries.column(block);
    ShareableBooleanFormula oldSummary = incoming.getOrDefault(predecessor, null);

    publishPredSummary(incoming, pSummaryVersions);

    int expectedVersion = incrementVersion(block, pSummaryVersions);
    Collection<ShareableBooleanFormula> predecessorSummaries =
        Maps.filterKeys(incoming, block -> predecessor != block).values();

    return new ForwardAnalysis(
        block,
        oldSummary,
        newSummary,
        expectedVersion,
        predecessorSummaries,
        reached,
        algorithm,
        argcpa,
        messageFactory,
        logManager,
        shutdownNotifier);
  }

  /**
   * Publish an updated block summary to the global map of block summaries.<br>
   * Used by {@link #process(Table, Map, Set)}.
   *
   * @param predSummaries The global map of block summaries
   * @param versions      The global map of block summary versions
   * @throws RequestInvalidatedException The task for which {@link #process(Table, Map, Set)} called 
   * this method has been invalidated (see documentation for {@link #process(Table, Map, Set)}).
   * @see #process(Table, Map, Set)
   */
  private void publishPredSummary(
      final Map<Block, ShareableBooleanFormula> predSummaries, final Map<Block, Integer> versions)
      throws RequestInvalidatedException {
    assert Thread.currentThread().getName().equals(Scheduler.getThreadName())
        : "Only the central thread "
        + Scheduler.getThreadName()
        + " may call publishPredSummary()";

    if (predecessor == null) {
      return;
    }

    final int predVersion = versions.getOrDefault(predecessor, 0);

    if (expectedPredVersion < predVersion) {
      throw new RequestInvalidatedException();
    } else {
      versions.put(predecessor, expectedPredVersion);
      predSummaries.put(predecessor, newSummary);
    }
  }
}
