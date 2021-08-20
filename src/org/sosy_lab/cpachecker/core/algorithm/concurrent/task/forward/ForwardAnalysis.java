// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward;

import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
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
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.TaskExecutor;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.TaskInvalidatedException;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.TaskManager;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.TaskValidity;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

@Options(prefix = "concurrent.task.forward")
public class ForwardAnalysis implements Task {
  private static volatile Configuration forward = null;

  private final Block predecessor;
  private final Block block;

  private final int expectedPredVersion;
  private final ShareableBooleanFormula newSummary;
  private final ReachedSet reached;
  private final TaskManager taskManager;
  private final LogManager logManager;
  private final ShutdownNotifier shutdownNotifier;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManager bfMgr;
  private ShareableBooleanFormula oldSummary = null;
  private int expectedVersion = 0;
  private Collection<ShareableBooleanFormula> predecessorSummaries;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(description = "Configuration file for forward analysis during concurrent analysis.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configFile = null;

  private Algorithm algorithm = null;
  private BlockAwareCompositeCPA cpa = null;

  public ForwardAnalysis(
      @Nullable final Block pPredecessor,
      final Block pBlock,
      @Nullable final ShareableBooleanFormula pNewSummary,
      final int pExpectedPredVersion,
      final Configuration pConfig,
      final Specification pSpecification,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCFA,
      final TaskManager pTaskManager)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    pConfig.inject(this);
    loadForwardConfig();
    predecessor = pPredecessor;
    block = pBlock;
    taskManager = pTaskManager;
    logManager = pLogger;
    shutdownNotifier = pShutdownNotifier;
    expectedPredVersion = pExpectedPredVersion;

    CoreComponentsFactory factory =
        new CoreComponentsFactory(
            forward, logManager, pShutdownNotifier, new AggregatedReachedSets());
    reached = factory.createReachedSet();

    CompositeCPA compositeCpa = (CompositeCPA) factory.createCPA(pCFA, pSpecification);

    if (compositeCpa.retrieveWrappedCpa(PredicateCPA.class) == null) {
      throw new InvalidConfigurationException(
          "Forward analysis requires a composite CPA with predicateCPA as component CPA.");
    }
    if (compositeCpa.retrieveWrappedCpa(LocationCPA.class) == null) {
      throw new InvalidConfigurationException(
          "Forward analysis requires a composite CPA with locationCPA as component CPA.");
    }

    cpa =
        (BlockAwareCompositeCPA)
            BlockAwareCompositeCPA.factory()
                .setConfiguration(forward)
                .setLogger(pLogger)
                .setShutdownNotifier(pShutdownNotifier)
                .set(pCFA, CFA.class)
                .set(block, Block.class)
                .set(compositeCpa, CompositeCPA.class)
                .createInstance();

    algorithm = factory.createAlgorithm(cpa, pCFA, pSpecification);

    PredicateCPA predicateCPA = cpa.retrieveWrappedCpa(PredicateCPA.class);
    assert predicateCPA != null;
    formulaManager = predicateCPA.getSolver().getFormulaManager();
    bfMgr = formulaManager.getBooleanFormulaManager();

    newSummary =
        (pNewSummary == null)
            ? new ShareableBooleanFormula(formulaManager, bfMgr.makeTrue())
            : pNewSummary;
  }

  /**
   * Increment version count of the block summary for {@code pBlock}.<br>
   * Used by {@link #preprocess(Table, Map)}.
   *
   * @param pBlock The block for which to update the summary version
   * @param pVersions The global map of summary versions
   * @return The new summary version
   * @see #preprocess(Table, Map)
   */
  private static int incrementVersion(final Block pBlock, final Map<Block, Integer> pVersions) {
    assert Thread.currentThread().getName().equals(TaskExecutor.getThreadName())
        : "Only " + TaskExecutor.getThreadName() + " may call incrementVersion()";

    int currentVersion = pVersions.getOrDefault(pBlock, 0);
    pVersions.put(pBlock, ++currentVersion);
    return currentVersion;
  }

  private void loadForwardConfig() throws InvalidConfigurationException {
    if (forward == null) {
      synchronized (ForwardAnalysis.class) {
        if (forward == null) {
          if (configFile != null) {
            try {
              forward = Configuration.builder().loadFromFile(configFile).build();
            } catch (IOException ignored) {
              logManager.log(
                  Level.SEVERE,
                  "Failed to load file ",
                  configFile,
                  ". Using default configuration.");
            }
          }

          if (forward == null) {
            forward =
                Configuration.builder()
                    .loadFromResource(ForwardAnalysis.class, "predicateForward.properties")
                    .build();
          }
        }
      }
    }
  }

  @Override
  public AlgorithmStatus call()
      throws CPAException, InterruptedException, InvalidConfigurationException {
    if (!summaryHasChanged()) {
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    Precision precision = cpa.getInitialPrecision(block.getEntry(), getDefaultPartition());
    AbstractState rawInitialState = null;
    while (rawInitialState == null) {
      try {
        rawInitialState = cpa.getInitialState(block.getEntry(), getDefaultPartition());
      } catch (InterruptedException ignored) {
        shutdownNotifier.shutdownIfNecessary();
      }
    }

    PredicateAbstractState rawPredicateState =
        AbstractStates.extractStateByType(rawInitialState, PredicateAbstractState.class);
    assert rawPredicateState != null;

    BooleanFormula cumPredSummary;
    if (predecessorSummaries.isEmpty()) {
      cumPredSummary = bfMgr.makeTrue();
    } else {
      List<BooleanFormula> summaries =
          predecessorSummaries.stream()
              .map(summary -> summary.getFor(formulaManager))
              .collect(Collectors.toList());

      cumPredSummary = bfMgr.or(summaries);
    }

    if (oldSummary != null) {
      BooleanFormula addedContext =
          bfMgr.and(
              oldSummary.getFor(formulaManager), bfMgr.not(newSummary.getFor(formulaManager)));
      BooleanFormula relevantChange = bfMgr.implication(cumPredSummary, addedContext);
      if (bfMgr.isFalse(relevantChange)) {
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }
    }

    BooleanFormula newContext = bfMgr.or(cumPredSummary, newSummary.getFor(formulaManager));
    PathFormula context = rawPredicateState.getPathFormula().withFormula(newContext);

    PredicateAbstractState predicateState =
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(context, rawPredicateState);

    List<AbstractState> componentStates = new ArrayList<>();
    for (ConfigurableProgramAnalysis componentCPA : cpa.getWrappedCPAs()) {
      AbstractState componentState = null;
      if (componentCPA instanceof PredicateCPA) {
        componentState = predicateState;
      } else {
        while (componentState == null) {
          try {
            componentState = componentCPA.getInitialState(block.getEntry(), getDefaultPartition());
          } catch (InterruptedException ignored) {
            shutdownNotifier.shutdownIfNecessary();
          }
        }
      }
      componentStates.add(componentState);
    }

    AbstractState entryState = new CompositeState(componentStates);
    reached.add(entryState, precision);

    logManager.log(Level.FINE, "Starting ForwardAnalysis on ", block);
    AlgorithmStatus status = algorithm.run(reached);

    for (final AbstractState state : reached.asCollection()) {
      if (AbstractStates.isTargetState(state)) {
        logManager.log(Level.FINE, "Target State:", state);
      }

      LocationState location = AbstractStates.extractStateByType(state, LocationState.class);
      assert location != null;

      if (block.getExits().containsKey(location.getLocationNode())) {
        PredicateAbstractState predState =
            AbstractStates.extractStateByType(state, PredicateAbstractState.class);
        assert predState != null;

        BooleanFormula exitFormula = predState.getPathFormula().getFormula();

        Block exit = block.getExits().get(location.getLocationNode());
        final ShareableBooleanFormula shareableFormula =
            new ShareableBooleanFormula(formulaManager, exitFormula);

        taskManager.spawnForwardAnalysis(block, expectedVersion, exit, shareableFormula);
      }
    }

    logManager.log(Level.FINE, "Completed ForwardAnalysis on ", block);
    return status.update(AlgorithmStatus.NO_PROPERTY_CHECKED);
  }

  private boolean summaryHasChanged() {
    if (oldSummary == null || newSummary == null) {
      return true;
    }

    BooleanFormula newFormula = newSummary.getFor(formulaManager);
    BooleanFormula oldFormula = oldSummary.getFor(formulaManager);
    BooleanFormula equivalence = bfMgr.equivalence(newFormula, oldFormula);

    return bfMgr.isFalse(equivalence);
  }

  /**
   * {@link #preprocess(Table, Map)} executes in the context of the central scheduler thread and
   * operates with exclusive and thread-safe access on the global map of calculated block summaries
   * and summary version counters. Each task type overwrites this method to implement global
   * synchronization using these structures.
   *
   * <p>For {@link ForwardAnalysis}, this method performs the following steps:
   *
   * <ol>
   *   <li>Publish updated block summary of predecessor block, if applicable.<br>
   *       In most cases, a new ForwardAnalysis <em>t1</em> on a block <em>b1</em> has been
   *       triggered by a completed ForwardAnalysis <em>t0</em> on a block <em>b0</em>, where
   *       <em>b0</em> belongs to the set of predecessors of <em>b1</em>. This other analysis
   *       <em>t0</em> usually identified a new block summary <em>Q0</em> for <em>b0</em>. Because
   *       the tasks themselves don't gain access to the global synchronization structures, {@link
   *       #preprocess(Table, Map)} running for <em>t1</em> offers the first opportunity after the
   *       completion of <em>t0</em> to access the global map of calculated summaries and publish
   *       <em>Q0</em> to it. Before doing so, {@link #preprocess(Table, Map)} running for
   *       <em>t1</em> first checks the expected version of the summary for <em>b0</em> against the
   *       value found in the global map of version counters. At the time the {@link
   *       ForwardAnalysis} <em>t1</em> got created by <em>t0</em>, <em>t0</em> provided <em>t1</em>
   *       with the version applicable when <em>t0</em> was scheduled. If {@link #preprocess(Table,
   *       Map)} for <em>t1</em> still encounters the same version in the global map, the updated
   *       summary <em>Q0</em> found by <em>t0</em> remains valid and must be published. If the
   *       version has been increment further in the meantime, the calculation of a newer version
   *       for the block summary of <em>b0</em> has been scheduled with a {@link ForwardAnalysis}
   *       <em>t2</em>. Furthermore, <em>t2</em> might even already have completed, and its result
   *       might have been published to the global map of block summaries. In this case, {@link
   *       #preprocess(Table, Map)} for <em>t1</em> must not overwrite this value with its now
   *       outdated version of <em>Q0</em>. Such task <em>t2</em> would also already have created a
   *       new {@link ForwardAnalysis} on <em>b1</em>, which makes the present task <em>t1</em>
   *       redundant. In this case, preprocessing therefore aborts with {@link
   *       TaskValidity#INVALID}.
   *   <li>Store the old version of the predecessor summary whose update triggered the creation of
   *       the present task. As soon as the task actually executes, it uses the old value to check
   *       whether new and old formula actually differ. If they do not, it completes early and
   *       performs no new analysis.
   *   <li>Increment the summary version of the block this task operates on by one and store the new
   *       value in the task.
   *   <li>Retrieve the list of all predecessor summaries of the current block and store it in the
   *       task. As soon as the task executes, it uses these summaries to construct the cumulative
   *       predecessor summary.
   * </ol>
   *
   * @param summaries Global map of block summaries
   * @param versions Global map of block summary versions
   * @return {@link TaskValidity#VALID} if the task remains valid, {@link TaskValidity#INVALID} if
   *     the method has determined the task to be outdated and requests its cancellation
   */
  @Override
  public TaskValidity preprocess(
      final Table<Block, Block, ShareableBooleanFormula> summaries,
      final Map<Block, Integer> versions) {
    assert Thread.currentThread().getName().equals(TaskExecutor.getThreadName())
        : "Only " + TaskExecutor.getThreadName() + " may call preprocess()";

    final Map<Block, ShareableBooleanFormula> incoming = summaries.column(block);
    oldSummary = incoming.getOrDefault(predecessor, null);

    try {
      publishPredSummary(incoming, versions);
    } catch (final TaskInvalidatedException exception) {
      return TaskValidity.INVALID;
    }

    expectedVersion = incrementVersion(block, versions);
    predecessorSummaries =
        ImmutableList.copyOf(Maps.filterKeys(incoming, block -> predecessor != block).values());

    return TaskValidity.VALID;
  }

  /**
   * Publish an updated block summary to the global map of block summaries.<br>
   * Used by {@link #preprocess(Table, Map)}.
   *
   * @param predSummaries The global map of block summaries
   * @param versions The global map of block summary versions
   * @throws TaskInvalidatedException The task for which {@link #preprocess(Table, Map)} called this
   *     method has been invalidated (see documentation for {@link #preprocess(Table, Map)}).
   * @see #preprocess(Table, Map)
   */
  private void publishPredSummary(
      final Map<Block, ShareableBooleanFormula> predSummaries, final Map<Block, Integer> versions)
      throws TaskInvalidatedException {
    assert Thread.currentThread().getName().equals(TaskExecutor.getThreadName())
        : "Only the central thread "
            + TaskExecutor.getThreadName()
            + " may call publishPredSummary()";

    if (predecessor == null) {
      return;
    }

    final int predVersion = versions.getOrDefault(predecessor, 0);

    if (expectedPredVersion < predVersion) {
      throw new TaskInvalidatedException();
    } else {
      versions.put(predecessor, expectedPredVersion);
      predSummaries.put(predecessor, newSummary);
    }
  }
}
