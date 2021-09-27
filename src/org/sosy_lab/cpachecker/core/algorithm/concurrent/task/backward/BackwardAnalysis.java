// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward;

import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.makeName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class BackwardAnalysis implements Task {
  private final Block target;
  private final CFANode start;
  private final PathFormula errorCondition;
  private final PathFormula blockSummary;
  private final ReachedSet reached;
  private final Algorithm algorithm;
  private final BlockAwareCompositeCPA cpa;
  private final Solver solver;
  private final FormulaManagerView fMgr;
  private final PathFormulaManager pfMgr;
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
      final TaskManager pTaskManager,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) {
    cpa = pCPA;
    PredicateCPA predicateCPA = cpa.retrieveWrappedCpa(PredicateCPA.class);
    assert predicateCPA != null;

    solver = predicateCPA.getSolver();
    fMgr = solver.getFormulaManager();
    pfMgr = predicateCPA.getPathFormulaManager();

    target = pBlock;
    start = pStart;
    errorCondition = pErrorCondition.getFor(fMgr, pfMgr);
    blockSummary = pBlockSummary.getFor(fMgr, pfMgr);

    reached = pReachedSet;
    algorithm = pAlgorithm;
    taskManager = pTaskManager;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }

  private void processReachedState(final AbstractState state)
      throws InterruptedException, CPAException, InvalidConfigurationException {
    LocationState location = extractStateByType(state, LocationState.class);
    assert location != null;

    CFANode node = location.getLocationNode();

    if (location.getLocationNode() == target.getEntry()) {
      PredicateAbstractState predState =
          extractStateByType(state, PredicateAbstractState.class);
      assert predState != null;

      PathFormula condition;
      if (predState.isAbstractionState()) {
        condition = predState.getAbstractionFormula().getBlockFormula();
      } else {
        condition = predState.getPathFormula();
      }

      ShareableBooleanFormula shareableCondition =
          new ShareableBooleanFormula(fMgr, condition);

      for (final Block predecessor : target.getPredecessors()) {
        taskManager.spawnBackwardAnalysis(predecessor, node, target, shareableCondition);
      }

      if (target.getPredecessors().isEmpty()) {
        try {
          if (solver.isUnsat(condition.getFormula())) {
            logManager.log(Level.INFO, "Verdict: Error condition unsatisfiable", condition);
          } else {
            logManager.log(Level.INFO, "Verdict: Satisfiable error condition!", condition);
          }
        } catch (SolverException ignored) {
          logManager.log(Level.WARNING, "Unhandled Solver Exception!");
        }
      }
    }
  }

  @Override
  public AlgorithmStatus call() throws Exception {
    PathFormula condition = stitchIndicesTogether(blockSummary, errorCondition);
    BooleanFormula reachable = fMgr.makeAnd(blockSummary.getFormula(), condition.getFormula());
    if (solver.isUnsat(reachable)) {
      logManager.log(Level.INFO, "Verdict: Swallowed error condition: ", errorCondition.getFormula());
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    CompositeState entryState = buildEntryState();
    Precision precision = cpa.getInitialPrecision(start, getDefaultPartition());
    reached.add(entryState, precision);

    logManager.log(Level.FINE, "Starting BackwardAnalysis on ", target);
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    do {
      AlgorithmStatus newStatus = algorithm.run(reached);
      status = status.update(newStatus);
      
      for (final AbstractState reachedState : reached.asCollection()) {
        processReachedState(reachedState);
      }

      Collection<AbstractState> waiting = new ArrayList<>(reached.getWaitlist());          
      reached.clear();
      for (final AbstractState waitingState : waiting) {
        CFANode location = AbstractStates.extractLocation(waitingState);
        assert location != null;

        if(location != target.getEntry() || location.isLoopStart()) {
          reached.add(waitingState, cpa.getInitialPrecision(location, getDefaultPartition())); 
        }
      }

      shutdownNotifier.shutdownIfNecessary();
    } while (!reached.getWaitlist().isEmpty());

    logManager.log(Level.FINE, "Completed BackwardAnalysis on ", target);
    return status;
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
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        errorCondition, rawPredicateState);
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
        extractStateByType(rawInitialState, PredicateAbstractState.class);
    assert rawPredicateState != null;

    return rawPredicateState;
  }

  /**
   * Todo: Merge Pointer Target Sets
   */
  private PathFormula stitchIndicesTogether(final PathFormula lower, final PathFormula upper) {
    BooleanFormula targetRaw = upper.getFormula();

    SSAMap lowerSSA = lower.getSsa();
    SSAMap upperSSA = upper.getSsa();

    Map<String, String> replacements = new HashMap<>();
    SSAMapBuilder newSSABuilder = upperSSA.builder();

    for (final String name : lowerSSA.allVariables()) {
      int indexBase = lowerSSA.getIndex(name);

      if (upperSSA.containsVariable(name)) {
        int maxUpperIndex = upperSSA.getIndex(name);
        int diff = indexBase - 1;

        for (int index = 1; index <= maxUpperIndex; ++index) {
          replacements.put(makeName(name, index), makeName(name, index + diff));
        }

        final CType type = upperSSA.getType(name);
        newSSABuilder = newSSABuilder.setIndex(name, type, maxUpperIndex + diff);
      }
    }

    if (replacements.isEmpty()) {
      return upper;
    }

    targetRaw = fMgr.renameFreeVariablesAndUFs(targetRaw,
        (pName) -> replacements.getOrDefault(pName, pName));
    SSAMap newSSA = newSSABuilder.build();

    PathFormula result = pfMgr.makeEmptyPathFormula();
    return result.withFormula(targetRaw).withContext(newSSA, upper.getPointerTargetSet());
  }
}
