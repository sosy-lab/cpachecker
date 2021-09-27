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

import java.util.ArrayList;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;

public class BackwardAnalysisContinuation implements Task {
  private final Block target;
  private final ReachedSet reached;
  private final Algorithm algorithm;
  private final BlockAwareCompositeCPA cpa;
  private final Solver solver;
  private final FormulaManagerView fMgr;
  private final TaskManager taskManager;
  private final LogManager logManager;
  private final ShutdownNotifier shutdownNotifier;

  BackwardAnalysisContinuation(
      final Block pBlock,
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
    target = pBlock;

    reached = pReachedSet;
    algorithm = pAlgorithm;
    taskManager = pTaskManager;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }
  
  @Override
  public AlgorithmStatus call() throws Exception {
    logManager.log(Level.FINE, "Continuing BackwardAnalysis on ", target);
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

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
    if (!reached.getWaitlist().isEmpty()) {
      taskManager.spawnBackwardAnalysisContinuation(target, reached, algorithm, cpa);
    }

    logManager.log(Level.FINE, "Completed BackwardAnalysis on ", target);
    return status;
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
}
