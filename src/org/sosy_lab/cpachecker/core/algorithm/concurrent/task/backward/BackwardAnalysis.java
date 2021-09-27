// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward;

import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class BackwardAnalysis implements Task {
  private final Block target;
  private final CFANode start;
  private final BooleanFormula errorCondition;
  private final BooleanFormula blockSummary;
  private final ReachedSet reached;
  private final Algorithm algorithm;
  private final BlockAwareCompositeCPA cpa;
  private final Solver solver;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManager bfMgr;
  private final TaskManager taskManager;
  private final LogManager logManager;
  private final ShutdownNotifier shutdownNotifier;

  BackwardAnalysis(
      final Block pBlock,
      final CFANode pStart,
      final ShareableBooleanFormula pErrorCondition,
      final ShareableBooleanFormula pBlockSummary,
      final ReachedSet pReachedSet,
      final Algorithm pAlgorithm,
      final BlockAwareCompositeCPA pCPA,
      final Solver pSolver,
      final TaskManager pTaskManager,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) {
    solver = pSolver;
    formulaManager = pSolver.getFormulaManager();
    bfMgr = formulaManager.getBooleanFormulaManager();

    target = pBlock;
    start = pStart;
    errorCondition = pErrorCondition.getFor(formulaManager);
    blockSummary = pBlockSummary.getFor(formulaManager);

    reached = pReachedSet;
    algorithm = pAlgorithm;
    cpa = pCPA;
    taskManager = pTaskManager;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }

  private void processReachedState(final AbstractState state)
      throws InterruptedException, CPAException, InvalidConfigurationException {
    LocationState location = AbstractStates.extractStateByType(state, LocationState.class);
    assert location != null;

    CFANode node = location.getLocationNode();

    if (location.getLocationNode() == target.getEntry()) {
      PredicateAbstractState predState =
          AbstractStates.extractStateByType(state, PredicateAbstractState.class);
      assert predState != null;

      BooleanFormula condition = predState.getPathFormula().getFormula();
      ShareableBooleanFormula shareableCondition =
          new ShareableBooleanFormula(formulaManager, condition);

      for (final Block predecessor : target.getPredecessors()) {
        taskManager.spawnBackwardAnalysis(predecessor, node, target, shareableCondition);
      }

      if (target.getPredecessors().isEmpty()) {
        try {
          if (solver.isUnsat(condition)) {
            logManager.log(Level.INFO, "Error condition unsatisfiable", condition);
          } else {
            logManager.log(Level.INFO, "Satisfiable error condition!", condition);
          }
        } catch (SolverException ignored) {
          logManager.log(Level.WARNING, "Unhandled Solver Exception!");
        }
      }
    }
  }

  @Override
  public AlgorithmStatus call() throws Exception {
    BooleanFormula entryCondition = bfMgr.and(blockSummary, errorCondition);
    if (solver.isUnsat(entryCondition)) {
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    CompositeState entryState = buildEntryState();
    Precision precision = cpa.getInitialPrecision(start, getDefaultPartition());
    reached.add(entryState, precision);

    logManager.log(Level.FINE, "Starting BackwardAnalysis on ", target);
    AlgorithmStatus status = algorithm.run(reached);

    for (final AbstractState state : reached.asCollection()) {
      processReachedState(state);
    }

    logManager.log(Level.FINE, "Completed BackwardAnalysis on ", target);
    return status.update(AlgorithmStatus.NO_PROPERTY_CHECKED);
  }

  private CompositeState buildEntryState() throws InterruptedException {
    PredicateAbstractState predicateEntryState = buildPredicateEntryState();

    List<AbstractState> componentStates = new ArrayList<>();
    for (ConfigurableProgramAnalysis componentCPA : cpa.getWrappedCPAs()) {
      AbstractState componentState = null;
      if (componentCPA instanceof PredicateCPA) {
        componentState = predicateEntryState;
      } else {
        while (componentState == null) {
          try {
            componentState = componentCPA.getInitialState(start, getDefaultPartition());
          } catch (InterruptedException ignored) {
            shutdownNotifier.shutdownIfNecessary();
          }
        }
      }
      componentStates.add(componentState);
    }

    return new CompositeState(componentStates);
  }

  private PredicateAbstractState buildPredicateEntryState() throws InterruptedException {
    PredicateAbstractState rawPredicateState = getRawPredicateEntryState();
    PathFormula context = rawPredicateState.getPathFormula().withFormula(errorCondition);

    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        context, rawPredicateState);
  }

  private PredicateAbstractState getRawPredicateEntryState() throws InterruptedException {
    AbstractState rawInitialState = null;
    while (rawInitialState == null) {
      try {
        rawInitialState = cpa.getInitialState(start, getDefaultPartition());
      } catch (InterruptedException ignored) {
        shutdownNotifier.shutdownIfNecessary();
      }
    }

    PredicateAbstractState rawPredicateState =
        AbstractStates.extractStateByType(rawInitialState, PredicateAbstractState.class);
    assert rawPredicateState != null;

    return rawPredicateState;
  }
}
