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
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class ForwardAnalysis implements Task {
  private final Block target;
  private final PathFormula newSummary;
  private final PathFormula oldSummary;
  private final int expectedVersion;
  private final ImmutableList<PathFormula> predecessorSummaries;
  private final ReachedSet reached;
  private final Algorithm algorithm;
  private final BlockAwareCompositeCPA cpa;
  private final Solver solver;
  private final FormulaManagerView fMgr;
  private final BooleanFormulaManagerView bfMgr;
  private final PathFormulaManager pfMgr;
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
      final TaskManager pTaskManager,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) {
    PredicateCPA predCPA = pCPA.retrieveWrappedCpa(PredicateCPA.class);
    assert predCPA != null;

    solver = predCPA.getSolver();
    fMgr = solver.getFormulaManager();
    bfMgr = fMgr.getBooleanFormulaManager();
    pfMgr = predCPA.getPathFormulaManager();
    
    target = pTarget;
    oldSummary = pOldSummary == null ? null : pOldSummary.getFor(fMgr, pfMgr);
    newSummary = pNewSummary == null ? null : pNewSummary.getFor(fMgr, pfMgr);
    expectedVersion = pExpectedVersion;
    predecessorSummaries =
        pPredecessorSummaries.stream()
            .map(formula -> formula.getFor(fMgr, pfMgr))
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

    PathFormula cumPredSummary = buildCumulativePredecessorSummary();
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

  private boolean thereIsNoRelevantChange(final PathFormula cumPredSummary)
      throws SolverException, InterruptedException {
    if (oldSummary == null || newSummary == null) {
      return false;
    }
    
    BooleanFormula addedContext 
        = bfMgr.and(oldSummary.getFormula(), bfMgr.not(newSummary.getFormula()));
    BooleanFormula relevantChange = bfMgr.implication(cumPredSummary.getFormula(), addedContext);
    
    return solver.isUnsat(relevantChange);
  }

  private PathFormula buildCumulativePredecessorSummary() throws InterruptedException {
    PathFormula cumPredSummary = pfMgr.makeEmptyPathFormula();
    for(final PathFormula formula : predecessorSummaries) {
      cumPredSummary = pfMgr.makeOr(cumPredSummary, formula); 
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

    BooleanFormula newRaw = newSummary.getFormula();
    BooleanFormula oldRaw = oldSummary.getFormula();
    BooleanFormula equivalence = bfMgr.equivalence(newRaw, oldRaw);
    return !solver.isUnsat(equivalence);
  }

  private CompositeState buildEntryState(final PathFormula cumPredSummary)
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

  private PredicateAbstractState buildPredicateEntryState(final PathFormula cumPredSummary)
      throws InterruptedException {
    PathFormula newContext = pfMgr.makeOr(cumPredSummary, newSummary);

    PredicateAbstractState rawPredicateState = getRawPredicateEntryState();

    return mkNonAbstractionStateWithNewPathFormula(
        newContext, rawPredicateState);
  }

  private void handleTargetStates()
      throws InterruptedException, CPAException, InvalidConfigurationException {
    for (final AbstractState state : reached.asCollection()) {
      CFANode location = extractLocation(state);
      assert location != null;

      if (isTargetState(state)) {
        logManager.log(Level.FINE, "Target State:", state);
        taskManager.spawnBackwardAnalysis(target, location);
      }
    }
  }

  private void propagateThroughExits()
      throws InterruptedException, CPAException, InvalidConfigurationException {
    for (final CFANode exit : target.getExits().keySet()) {
      PathFormula exitFormula = pfMgr.makeEmptyPathFormula();

      for (final AbstractState exitState : filterLocation(reached, exit)) {
        final PredicateAbstractState predState =
            extractStateByType(exitState, PredicateAbstractState.class);
        assert predState != null;
        
        exitFormula = pfMgr.makeOr(exitFormula, predState.getPathFormula());
      }
      
      Block successor = target.getExits().get(exit);
      final ShareableBooleanFormula shareableFormula =
          new ShareableBooleanFormula(fMgr, exitFormula);

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
