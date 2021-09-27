// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward;

import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;
import static org.sosy_lab.cpachecker.util.AbstractStates.filterLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.isTargetState;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.TaskManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class ForwardAnalysis implements Task {
  private final Block target;
  private final BooleanFormula newSummary;
  private final BooleanFormula oldSummary;
  private final int expectedVersion;
  private final ImmutableList<BooleanFormula> predecessorSummaries;
  private final ReachedSet reached;
  private final Algorithm algorithm;
  private final BlockAwareCompositeCPA cpa;
  private final Solver solver;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManager bfMgr;
  private final TaskManager taskManager;
  private final LogManager logManager;
  private final ShutdownNotifier shutdownNotifier;

  public ForwardAnalysis(
      final Block pTarget,
      @Nullable final ShareableBooleanFormula pOldSummary,
      @Nullable final ShareableBooleanFormula pNewSummary,
      final int pExpectedVersion,
      final Collection<ShareableBooleanFormula> pPredecessorSummaries,
      final ReachedSet pReachedSet,
      final Algorithm pAlgorithm,
      final BlockAwareCompositeCPA pCPA,
      final Solver pSolver,
      final TaskManager pTaskManager,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) {
    solver = pSolver;
    formulaManager = solver.getFormulaManager();
    bfMgr = formulaManager.getBooleanFormulaManager();
    target = pTarget;
    oldSummary = pOldSummary == null ? null : pOldSummary.getFor(formulaManager);
    newSummary = pNewSummary == null ? null : pNewSummary.getFor(formulaManager);
    expectedVersion = pExpectedVersion;
    predecessorSummaries =
        pPredecessorSummaries.stream()
            .map(formula -> formula.getFor(formulaManager))
            .collect(ImmutableList.toImmutableList());

    reached = pReachedSet;
    algorithm = pAlgorithm;
    cpa = pCPA;
    taskManager = pTaskManager;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public AlgorithmStatus call()
      throws CPAException, InterruptedException, InvalidConfigurationException, SolverException {
    AlgorithmStatus status = AlgorithmStatus.NO_PROPERTY_CHECKED;
    if (isSummaryUnchanged()) {
      return status;
    }

    BooleanFormula cumPredSummary = buildCumulativePredecessorSummary();
    if (thereIsNoRelevantChange(cumPredSummary)) {
      return status;
    }

    CompositeState entryState = buildEntryState(cumPredSummary);
    Precision precision = cpa.getInitialPrecision(target.getEntry(), getDefaultPartition());
    reached.add(entryState, precision);

    logManager.log(Level.FINE, "Starting ForwardAnalysis on ", target);
    do {
      status = status.update(algorithm.run(reached));

      for (final AbstractState waiting : reached.getWaitlist()) {
        if (isTargetState(waiting)) {
          reached.removeOnlyFromWaitlist(waiting);

          final Pair<CompositeState, Precision> reset = resetAutomata((CompositeState) waiting);
          assert reset.getFirst() != null && reset.getSecond() != null;

          reached.add(reset.getFirst(), reset.getSecond());
        }
      }

    } while (reached.hasWaitingState());

    handleTargetStates();
    propagateThroughExits();

    logManager.log(Level.FINE, "Completed ForwardAnalysis on ", target);
    return status.update(AlgorithmStatus.NO_PROPERTY_CHECKED);
  }

  private boolean thereIsNoRelevantChange(final BooleanFormula cumPredSummary)
      throws SolverException, InterruptedException {
    if (oldSummary == null || newSummary == null) {
      return false;
    }

    BooleanFormula addedContext = bfMgr.and(oldSummary, bfMgr.not(newSummary));
    BooleanFormula relevantChange = bfMgr.implication(cumPredSummary, addedContext);

    return solver.isUnsat(relevantChange);
  }

  private BooleanFormula buildCumulativePredecessorSummary() {
    BooleanFormula cumPredSummary;
    if (predecessorSummaries.isEmpty()) {
      cumPredSummary = bfMgr.makeTrue();
    } else {
      cumPredSummary = bfMgr.or(predecessorSummaries);
    }

    return cumPredSummary;
  }

  private PredicateAbstractState getRawPredicateEntryState() throws InterruptedException {
    AbstractState rawInitialState = null;
    while (rawInitialState == null) {
      try {
        rawInitialState = cpa.getInitialState(target.getEntry(), getDefaultPartition());
      } catch (InterruptedException ignored) {
        shutdownNotifier.shutdownIfNecessary();
      }
    }

    PredicateAbstractState rawPredicateState =
        extractStateByType(rawInitialState, PredicateAbstractState.class);
    assert rawPredicateState != null;

    return rawPredicateState;
  }

  private boolean isSummaryUnchanged() throws SolverException, InterruptedException {
    if (oldSummary == null || newSummary == null) {
      return false;
    }

    BooleanFormula equivalence = bfMgr.equivalence(newSummary, oldSummary);
    return !solver.isUnsat(equivalence);
  }

  private CompositeState buildEntryState(final BooleanFormula cumPredSummary)
      throws InterruptedException {
    PredicateAbstractState predicateEntryState = buildPredicateEntryState(cumPredSummary);

    List<AbstractState> componentStates = new ArrayList<>();
    for (ConfigurableProgramAnalysis componentCPA : cpa.getWrappedCPAs()) {
      AbstractState componentState = null;
      if (componentCPA instanceof PredicateCPA) {
        componentState = predicateEntryState;
      } else {
        while (componentState == null) {
          try {
            componentState = componentCPA.getInitialState(target.getEntry(), getDefaultPartition());
          } catch (InterruptedException ignored) {
            shutdownNotifier.shutdownIfNecessary();
          }
        }
      }
      componentStates.add(componentState);
    }

    return new CompositeState(componentStates);
  }

  private PredicateAbstractState buildPredicateEntryState(final BooleanFormula cumPredSummary)
      throws InterruptedException {
    BooleanFormula newContext = bfMgr.or(cumPredSummary, newSummary);

    PredicateAbstractState rawPredicateState = getRawPredicateEntryState();
    PathFormula context = rawPredicateState.getPathFormula().withFormula(newContext);

    return mkNonAbstractionStateWithNewPathFormula(
        context, rawPredicateState);
  }

  private void processReachedState(final AbstractState state)
      throws InterruptedException, InvalidConfigurationException, CPAException {
    if (AbstractStates.isTargetState(state)) {
      logManager.log(Level.FINE, "Target State:", state);
    }

    LocationState location = AbstractStates.extractStateByType(state, LocationState.class);
    assert location != null;

    if (AbstractStates.isTargetState(state)) {
      taskManager.spawnBackwardAnalysis(target, location.getLocationNode());
    }

    if (target.getExits().containsKey(location.getLocationNode())) {
      PredicateAbstractState predState =
          AbstractStates.extractStateByType(state, PredicateAbstractState.class);
      assert predState != null;

      BooleanFormula exitFormula = predState.getPathFormula().getFormula();

      Block exit = target.getExits().get(location.getLocationNode());
      final ShareableBooleanFormula shareableFormula =
          new ShareableBooleanFormula(formulaManager, exitFormula);

      taskManager.spawnForwardAnalysis(target, expectedVersion, successor, shareableFormula);
    }
  }

  private Pair<CompositeState, Precision> resetAutomata(final CompositeState state)
      throws InterruptedException {
    List<AbstractState> componentStates = new ArrayList<>();
    for (final AbstractState wrappedState : state.getWrappedStates()) {
      if (!(wrappedState instanceof AutomatonState)) {
        componentStates.add(wrappedState);
      }
    }

    CFANode location = extractLocation(state);
    assert location != null;

    CompositeState initialState =
        (CompositeState) cpa.getInitialState(location, getDefaultPartition());

    for (final AbstractState wrappedInitialState : initialState.getWrappedStates()) {
      if (wrappedInitialState instanceof AutomatonState) {
        componentStates.add(wrappedInitialState);
      }
    }

    Precision precision = cpa.getInitialPrecision(location, getDefaultPartition());
    return Pair.of(new CompositeState(componentStates), precision);
  }
}
