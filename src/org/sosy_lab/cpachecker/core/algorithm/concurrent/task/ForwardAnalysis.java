// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

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

  private final int expectedPredecessorVersion;
  private final ShareableBooleanFormula newSummary;
  private final ReachedSet reached;
  private final TaskManager taskManager;
  private final LogManager logManager;
  private final ShutdownNotifier shutdownNotifier;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManager bfMgr;
  private int expectedVersion = 0;
  private ShareableBooleanFormula oldSummary = null;
  private Collection<ShareableBooleanFormula> predecessorSummaries;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(description = "Configuration file for forward analysis during concurrent analysis.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configFile = null;

  private Algorithm algorithm = null;
  private BlockAwareCompositeCPA cpa = null;

  protected ForwardAnalysis(
      @Nullable final Block pPredecessor,
      final Block pBlock,
      @Nullable final ShareableBooleanFormula pNewSummary,
      final int pExpectedPredecessorVersion,
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
    expectedPredecessorVersion = pExpectedPredecessorVersion;

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
    if (!summaryChanged()) {
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

  private boolean summaryChanged() {
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
   * operates with exclusive, thread-safe access on the global maps of block summaries and block
   * summary versions.
   *
   * <p>For {@link ForwardAnalysis}, it performs the following steps:
   *
   * <ol>
   *   <li>Check whether the version of updated summary of the predecessor block which triggered
   *       this task is still lower than the version stored when the ForwardAnalysis which
   *       calculated the updated summary was created. Only in this case, the new summary which
   *       forms the basis for the present task instance remains valid. If the version has been
   *       increment in the meantime, a newer version of the block summary has been set and must not
   *       be overwritten by the outdated version stored in this task. In this case, preprocessing
   *       aborts with {@link TaskValidity#INVALID}.
   *   <li>Store the old version of the predecessor summary in the task. As soon as the task
   *       actually executes, it uses the old value to check whether new and old formula actually
   *       differ. If they do not, it completes early and performs no new analysis.
   *   <li>Update summary and summary version for the predecessor block in the global maps.
   *   <li>Increment the summary version of the present block by one and store the new value in the
   *       task.
   *   <li>Retrieve the list of all predecessor summaries of the current block and store it in the
   *       task. As soon as the task executes, it uses these summaries to construct the cumulative
   *       predecessor summary.
   * </ol>
   *
   * @param summaries Global map of block summaries
   * @param versions Global map of block summary versions
   * @return {@link TaskValidity#VALID} if the task remains valid, {@link TaskValidity#INVALID} if
   *     preprocessing has determined the task to be outdated and requests its cancellation
   */
  @Override
  public TaskValidity preprocess(
      Table<Block, Block, ShareableBooleanFormula> summaries, Map<Block, Integer> versions) {
    Map<Block, ShareableBooleanFormula> incomingSummaries = summaries.column(block);

    if (predecessor != null) {
      final int currentPredecessorVersion = versions.getOrDefault(predecessor, 0);
      oldSummary = incomingSummaries.get(predecessor);

      if (expectedPredecessorVersion < currentPredecessorVersion) {
        return TaskValidity.INVALID;
      } else {
        versions.put(predecessor, expectedPredecessorVersion);
        incomingSummaries.put(predecessor, newSummary);
      }
    }

    int currentVersion = versions.getOrDefault(block, 0);
    versions.put(block, ++currentVersion);
    expectedVersion = currentVersion;

    predecessorSummaries =
        ImmutableList.copyOf(
            Maps.filterKeys(incomingSummaries, block -> predecessor == block).values());

    return TaskValidity.VALID;
  }
}
