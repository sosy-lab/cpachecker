// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward;

import static org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus.SOUND_AND_PRECISE;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;
import static org.sosy_lab.cpachecker.util.AbstractStates.filterLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.isTargetState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "concurrent.task.forward")
public class ForwardAnalysisCore extends Task {
  private final Block target;
  private final Solver solver;
  private final int expectedVersion;
  private final FormulaManagerView fMgr;
  private final PathFormulaManager pfMgr;
  
  private final ForwardAnalysisCoreStatistics statistics;
  
  private AlgorithmStatus status = SOUND_AND_PRECISE;
  private boolean hasCreatedContinuationRequest = false;
  
  public ForwardAnalysisCore(
      final Configuration pGlobalConfiguration,
      final Block pTarget,
      final ReachedSet pReachedSet,
      final int pExpectedVersion,
      final Algorithm pAlgorithm,
      final ARGCPA pCPA,
      final Solver pSolver,
      final PathFormulaManager pPfMgr,
      final MessageFactory pMessageFactory,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    super(pCPA, pAlgorithm, pReachedSet, pMessageFactory, pLogManager, pShutdownNotifier);
    pGlobalConfiguration.inject(this);
    
    target = pTarget;
    expectedVersion = pExpectedVersion;
    solver = pSolver;
    fMgr = solver.getFormulaManager();
    pfMgr = pPfMgr;
    
    statistics = new ForwardAnalysisCoreStatistics(pTarget);
  }

  @Override
  protected void execute()
      throws CPAException, InterruptedException, InvalidConfigurationException, SolverException 
  {
    logManager.log(Level.FINE, "Starting ForwardAnalysisCore on ", target);

    AlgorithmStatus newStatus = algorithm.run(reached);
    status = status.update(newStatus);

    handleTargetStates();
    propagateThroughExits();
    resetReachedSet();    
    
    shutdownNotifier.shutdownIfNecessary();
    if(reached.hasWaitingState()) {
      hasCreatedContinuationRequest = true;
      messageFactory.sendForwardAnalysisContinuationRequest(
          target, expectedVersion, cpa, algorithm, reached, solver, pfMgr
      );
    }

    logManager.log(Level.FINE, "Completed ForwardAnalysisCore on ", target);
    messageFactory.sendTaskCompletedMessage(this, status, statistics);
  }
  
  private void resetReachedSet() throws InterruptedException, SolverException {
    Collection<AbstractState> waiting = new ArrayList<>(reached.getWaitlist());
    reached.clearWaitlist();
    
    for (final AbstractState waitingState : waiting) {
      final PredicateAbstractState predicateState
          = extractStateByType(waitingState, PredicateAbstractState.class);
      assert predicateState != null;

      BooleanFormula abstractionFormula = predicateState.getAbstractionFormula().asFormula();
      BooleanFormula pathFormula = predicateState.getPathFormula().getFormula();
      BooleanFormula formula = fMgr.makeAnd(abstractionFormula, pathFormula);
      
      if(!solver.isUnsat(formula)) {
        reached.add(waitingState, reached.getPrecision(waitingState));
      }
    }
  }
  
  private void handleTargetStates()
      throws InterruptedException, CPAException, InvalidConfigurationException {
    for (final AbstractState state : reached.asCollection()) {
      CFANode location = extractLocation(state);
      assert location != null;

      if (isTargetState(state) && targetIsError(state)) {
        logManager.log(Level.FINE, "Target State:", state);

        ErrorOrigin origin = ErrorOrigin.create(state, reached.getPrecision(state));
        messageFactory.sendBackwardAnalysisRequest(target, location, origin);
      }
    }
  }

  private boolean targetIsError(final AbstractState state) {
    assert state instanceof ARGState;
    ARGState argState = (ARGState) state;

    assert argState.getWrappedState() instanceof BlockAwareCompositeState;
    BlockAwareCompositeState blockAwareState = (BlockAwareCompositeState) argState.getWrappedState();
    
    for (final AbstractState componentState : blockAwareState.getWrappedStates()) {
      if (componentState instanceof Targetable) {
        Targetable targetableState = (Targetable) componentState;
        if (targetableState.isTarget() && targetableState instanceof AutomatonState) {
          return true;
        }
      }
    }

    return false;
  }

  private void propagateThroughExits()
      throws InterruptedException, CPAException, InvalidConfigurationException {
    for (Map.Entry<CFANode, Block> entry : target.getExits().entrySet()) {
      final CFANode exit = entry.getKey();
      
      PathFormula exitFormula = null;
      for (AbstractState exitState : filterLocation(reached, exit)) {
        final PredicateAbstractState predState =
            extractStateByType(exitState, PredicateAbstractState.class);
        assert predState != null;

        PathFormula partialExitFormula
            = pfMgr.makeAnd(predState.getPathFormula(), predState.getAbstractionFormula().asFormula());

        if (exitFormula == null) {
          exitFormula = partialExitFormula;
        } else {
          exitFormula = pfMgr.makeOr(exitFormula, partialExitFormula);
        }
      }
      
      assert exitFormula != null;
      
      Block successor = entry.getValue();
      final ShareableBooleanFormula shareableFormula = new ShareableBooleanFormula(fMgr, exitFormula);
      messageFactory.sendForwardAnalysisRequest(target, expectedVersion, successor, shareableFormula);
    }
  }

  @Override public String toString() {
    return "ForwardAnalysisCore on block with entry location " + target.getEntry();
  }

  public boolean hasCreatedContinuationRequest() {
    return hasCreatedContinuationRequest;
  }
}
