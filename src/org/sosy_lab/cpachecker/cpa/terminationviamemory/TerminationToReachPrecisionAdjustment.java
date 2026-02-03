// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

public class TerminationToReachPrecisionAdjustment implements PrecisionAdjustment {
  private final Solver solver;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaManagerView fmgr;
  private final InterpolationManager itpMgr;
  private final TerminationToReachStatistics statistics;
  private final CFA cfa;
  private final LogManager logger;

  public TerminationToReachPrecisionAdjustment(
      Solver pSolver,
      TerminationToReachStatistics pStatistics,
      LogManager plogger,
      CFA pCFA,
      BooleanFormulaManagerView pBfmgr,
      FormulaManagerView pFmgr,
      InterpolationManager pItpMgr) {
    solver = pSolver;
    statistics = pStatistics;
    cfa = pCFA;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    logger = plogger;
    itpMgr = pItpMgr;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState)
      throws CPAException, InterruptedException {
    TerminationToReachState terminationState = (TerminationToReachState) state;
    LocationState locationState = AbstractStates.extractStateByType(fullState, LocationState.class);
    CallstackState callstackState =
        AbstractStates.extractStateByType(fullState, CallstackState.class);
    CFANode location = AbstractStates.extractLocation(locationState);

    PrecisionAdjustmentResult result =
        new PrecisionAdjustmentResult(state, precision, Action.CONTINUE);
    Pair<LocationState, CallstackState> keyPair = Pair.of(locationState, callstackState);

    if (location.isLoopStart() && terminationState.getStoredValues().containsKey(keyPair)) {
      if (terminationState.getNumberOfIterationsAtLoopHead(keyPair) > 1) {
        boolean isTargetStateReachable;
        BooleanFormula prefixFormula =
            terminationState.getPathFormulasForPrefix().get(keyPair).getFormula();
        BooleanFormula iterationFormula =
            terminationState.getPathFormulasForIteration().get(keyPair).getFormula();
        BooleanFormula sameStateFormula =
            buildCycleFormula(
                terminationState.getStoredValues().get(keyPair),
                terminationState.getPathFormulasForIteration().get(keyPair).getSsa(),
                terminationState.getNumberOfIterationsAtLoopHead(keyPair) - 1);

        while (true) {
          // First, check that the BMC check is UNSATs
          try {
            isTargetStateReachable =
                !solver.isUnsat(bfmgr.and(prefixFormula, iterationFormula, sameStateFormula));
          } catch (SolverException e) {
            logger.logDebugException(e);
            return Optional.of(result);
          }
          if (isTargetStateReachable) {
            terminationState.makeTarget();
            result = result.withAbstractState(terminationState);
            statistics.setNonterminatingLoop(
                cfa.getLoopStructure().orElseThrow().getLoopsForLoopHead(location));
            return Optional.of(result.withAction(Action.BREAK));
          }

          // If BMC check is UNSAT, try to overapproximate the transition invariant
          ImmutableList<BooleanFormula> interpolant =
              itpMgr
                  .interpolate(
                      ImmutableList.of(
                          bfmgr.and(prefixFormula, iterationFormula), sameStateFormula))
                  .orElseThrow();
          prefixFormula = bfmgr.or(prefixFormula, interpolant.getFirst());

          // Check the fix-point, i.e. check whether the new interpolant is a transition invariant
          if (isTransitionInvariant(prefixFormula, iterationFormula, sameStateFormula)) {
            TerminationToReachState terminatingState =
                new TerminationToReachState(
                    ImmutableMap.of(),
                    ImmutableMap.of(),
                    ImmutableMap.of(),
                    ImmutableMap.of(),
                    prefixFormula);
            return Optional.of(result.withAbstractState(terminatingState));
          }
        }
      }
    }
    return Optional.of(result);
  }

  private boolean isTransitionInvariant(
      BooleanFormula candidateTransitionInvariant,
      BooleanFormula iterationFormula,
      BooleanFormula sameStateFormula) {
    return false;
  }

  private BooleanFormula buildCycleFormula(
      Map<Integer, ImmutableSet<Formula>> storedValues, SSAMap pLatestValues, int pMaxIndex) {
    BooleanFormula cycle =
        buildComparingFormulas(storedValues, pMaxIndex, pLatestValues).stream()
            .collect(bfmgr.toDisjunction());
    return cycle;
  }

  private ImmutableList<BooleanFormula> buildComparingFormulas(
      Map<Integer, ImmutableSet<Formula>> storedValues, int pMaxIndex, SSAMap pLatestValues) {
    ImmutableList.Builder<BooleanFormula> comparingFormulas = ImmutableList.builder();
    for (Entry<Integer, ImmutableSet<Formula>> savedVariables : storedValues.entrySet()) {
      if (savedVariables.getKey().intValue() >= pMaxIndex) {
        continue;
      }
      BooleanFormula comparingFormula = bfmgr.makeTrue();
      for (Formula oldVariable : savedVariables.getValue()) {
        comparingFormula =
            bfmgr.and(
                comparingFormula,
                fmgr.assignment(
                    fmgr.instantiate(fmgr.uninstantiate(oldVariable), pLatestValues), oldVariable));
      }
      comparingFormulas.add(comparingFormula);
    }
    return comparingFormulas.build();
  }
}
