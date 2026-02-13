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
import java.util.HashMap;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
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

  private final int PREV_VARIABLES_INDEX = 1;
  private final int CURR_VARIABLES_INDEX = 2;

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
        boolean isOverapproximating = false;
        PathFormula prefixPathFormula = terminationState.getPathFormulasForPrefix().orElseThrow();
        SSAMap smallestIndices = prefixPathFormula.getSsa();
        SSAMap largestIndices =
            terminationState.getPathFormulasForIteration().get(keyPair).getSsa();

        SSAMap prevIndices = initializeSSAMapWithIndex(largestIndices, PREV_VARIABLES_INDEX);
        SSAMap currIndices = initializeSSAMapWithIndex(smallestIndices, CURR_VARIABLES_INDEX);

        BooleanFormula prefixFormula = prefixPathFormula.getFormula();
        BooleanFormula iterationFormula =
            terminationState.getPathFormulasForIteration().get(keyPair).getFormula();
        ImmutableList<BooleanFormula> sameStateFormulas =
            buildCycleFormula(
                terminationState.getStoredValues().get(keyPair),
                largestIndices,
                terminationState.getNumberOfIterationsAtLoopHead(keyPair) - 1);
        while (true) {
          // First, check that the BMC check is UNSATs
          BooleanFormula latestSameStateFormula = bfmgr.makeTrue();
          for (BooleanFormula sameStateFormula : sameStateFormulas) {
            try {
              if (isOverapproximating) {
                sameStateFormula =
                    instantiateTransitionInvariant(sameStateFormula, prevIndices, largestIndices);
                prefixFormula =
                    instantiateTransitionInvariant(prefixFormula, prevIndices, smallestIndices);
              }
              isTargetStateReachable =
                  !solver.isUnsat(bfmgr.and(prefixFormula, iterationFormula, sameStateFormula));
            } catch (SolverException e) {
              logger.logDebugException(e);
              return Optional.of(result);
            }
            if (isTargetStateReachable) {
              if (!isOverapproximating) {
                terminationState.makeTarget();
                result = result.withAbstractState(terminationState);
                statistics.setNonterminatingLoop(
                    cfa.getLoopStructure().orElseThrow().getLoopsForLoopHead(location));
                result = result.withAction(Action.BREAK);
              }
              return Optional.of(result);
            }
            latestSameStateFormula = sameStateFormula;
            if (isOverapproximating) {
              break;
            }
          }

          // Check the fix-point, i.e. check whether the new interpolant is a transition invariant
          if (isOverapproximating
              && isTransitionInvariant(
                  prefixFormula, iterationFormula, prevIndices, smallestIndices, largestIndices)) {
            TerminationToReachState terminatingState =
                new TerminationToReachState(
                    ImmutableMap.of(),
                    ImmutableMap.of(),
                    ImmutableMap.of(),
                    Optional.empty(),
                    Optional.empty());
            terminatingState.setTerminating();
            return Optional.of(result.withAbstractState(terminatingState));
          }

          // If BMC check is UNSAT, try to overapproximate the transition invariant
          ImmutableList<BooleanFormula> interpolant =
              itpMgr
                  .interpolate(
                      ImmutableList.of(
                          bfmgr.and(prefixFormula, iterationFormula), latestSameStateFormula))
                  .orElseThrow();
          if (!isOverapproximating) {
            prefixFormula = bfmgr.makeFalse();
          }
          isOverapproximating = true;
          prefixFormula =
              bfmgr.or(
                  prefixFormula,
                  instantiateTransitionInvariant(interpolant.getFirst(), prevIndices, currIndices));
        }
      }
    }
    return Optional.of(result);
  }

  /**
   * We have to represent the transition invariant in the unified way. Therefore, we instantiate
   * variables representing the previous state with index 1 and the variables for the current state
   * with index 2.
   */
  private BooleanFormula instantiateTransitionInvariant(
      BooleanFormula candidateTransitionInvariant, SSAMap prevSSAMap, SSAMap currSSAMap) {
    Map<String, Formula> varNamesToFormulas = fmgr.extractVariables(candidateTransitionInvariant);
    Map<Formula, Integer> prevIndex = new HashMap<>();
    for (Entry<String, Formula> entry : varNamesToFormulas.entrySet()) {
      int index = getSSAIndex(entry.getKey());
      Formula pureVar = fmgr.uninstantiate(entry.getValue());
      if (!prevIndex.containsKey(pureVar) || prevIndex.get(pureVar) > index) {
        prevIndex.put(pureVar, index);
      }
    }

    ImmutableMap.Builder<Formula, Formula> prevSubMap = ImmutableMap.builder();
    ImmutableMap.Builder<Formula, Formula> currSubMap = ImmutableMap.builder();
    for (Entry<String, Formula> entry : varNamesToFormulas.entrySet()) {
      if (prevIndex.get(fmgr.uninstantiate(entry.getValue())) == getSSAIndex(entry.getKey())) {
        prevSubMap.put(
            entry.getValue(), fmgr.instantiate(fmgr.uninstantiate(entry.getValue()), prevSSAMap));
      } else {
        currSubMap.put(
            entry.getValue(), fmgr.instantiate(fmgr.uninstantiate(entry.getValue()), currSSAMap));
      }
    }
    candidateTransitionInvariant =
        fmgr.substitute(candidateTransitionInvariant, prevSubMap.buildOrThrow());
    candidateTransitionInvariant =
        fmgr.substitute(candidateTransitionInvariant, currSubMap.buildOrThrow());
    return candidateTransitionInvariant;
  }

  private SSAMap initializeSSAMapWithIndex(SSAMap pMap, int pI) {
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    for (String variable : pMap.allVariables()) {
      builder.setIndex(variable, pMap.getType(variable), pI);
    }
    return builder.build();
  }

  private int getSSAIndex(String pFormula) {
    return FormulaManagerView.parseName(pFormula).getSecond().orElse(-2);
  }

  private boolean isTransitionInvariant(
      BooleanFormula candidateTransitionInvariant,
      BooleanFormula iterationFormula,
      SSAMap mapForIndexTwo,
      SSAMap smallestIndicesInIteration,
      SSAMap largestIndicesInIteration) {
    boolean isTransitionInvariant;
    BooleanFormula firstStepInTransInv =
        instantiateTransitionInvariant(
            candidateTransitionInvariant, mapForIndexTwo, smallestIndicesInIteration);
    BooleanFormula secondStepInTransInv =
        instantiateTransitionInvariant(
            candidateTransitionInvariant, mapForIndexTwo, largestIndicesInIteration);
    try {
      isTransitionInvariant =
          solver.implies(bfmgr.and(firstStepInTransInv, iterationFormula), secondStepInTransInv);
    } catch (SolverException | InterruptedException e) {
      logger.logDebugException(e);
      return false;
    }
    if (isTransitionInvariant) {
      return true;
    }
    return false;
  }

  private ImmutableList<BooleanFormula> buildCycleFormula(
      Map<Integer, ImmutableSet<Formula>> storedValues, SSAMap pLatestValues, int pMaxIndex) {
    return buildComparingFormulas(storedValues, pMaxIndex, pLatestValues);
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
