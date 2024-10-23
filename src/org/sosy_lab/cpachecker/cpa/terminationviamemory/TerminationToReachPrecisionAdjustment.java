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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class TerminationToReachPrecisionAdjustment implements PrecisionAdjustment {
  private final Solver solver;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaManagerView fmgr;
  private final InterpolationManager itpMgr;
  private final CtoFormulaConverter ctoFormulaConverter;
  private final TerminationToReachStatistics statistics;
  private final CFA cfa;

  public TerminationToReachPrecisionAdjustment(
      Solver pSolver,
      TerminationToReachStatistics pStatistics,
      CFA pCFA,
      BooleanFormulaManagerView pBfmgr,
      FormulaManagerView pFmgr,
      PathFormulaManager pPathfmgr,
      CToFormulaConverterWithPointerAliasing pCtoFormulaConverter,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger)
      throws InvalidConfigurationException {
    solver = pSolver;
    statistics = pStatistics;
    cfa = pCFA;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    itpMgr = new InterpolationManager(
        pPathfmgr, solver, Optional.empty(), Optional.empty(), pConfig, pShutdownNotifier, pLogger);
    ctoFormulaConverter = pCtoFormulaConverter;
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
    CFANode location = AbstractStates.extractLocation(locationState);
    PredicateAbstractState predicateState =
        AbstractStates.extractStateByType(fullState, PredicateAbstractState.class);

    SSAMap ssaMap = predicateState.getPathFormula().getSsa();
    PrecisionAdjustmentResult result =
        new PrecisionAdjustmentResult(state, precision, Action.CONTINUE);

    if (location.isLoopStart() && terminationState.getStoredValues().containsKey(locationState)) {
      terminationState.putNewPathFormula(predicateState.getPathFormula().getFormula());

      for (int i = 0;
          i < terminationState.getNumberOfIterationsAtLoopHead(locationState) - 1;
          ++i) {
        boolean isTargetStateReachable = false;
        ImmutableList<BooleanFormula> targetFormula =
            buildCycleFormula(
                terminationState.getPathFormulas(),
                terminationState.getStoredValues().get(locationState),
                ssaMap,
                i);
        try {
          isTargetStateReachable = !solver.isUnsat(bfmgr.and(targetFormula));
        } catch (SolverException e) {
          continue;
        }
        if (isTargetStateReachable) {
          // This analysis may be imprecise if the program contains pointers. We return UNKNOWN
          // instead.
          // TODO: Implement pointers handling.
          if (programContainsPointers(ssaMap)) {
            return Optional.of(result.withAction(Action.BREAK));
          }
          terminationState.makeTarget();
          result = result.withAbstractState(terminationState);
          statistics.setNonterminatingLoop(
              cfa.getLoopStructure().orElseThrow().getLoopsForLoopHead(location));
          return Optional.of(result.withAction(Action.BREAK));
        } else {
          BooleanFormula interpolant = fmgr.uninstantiate(itpMgr.interpolate(targetFormula).get().get(0));

          // Fix-point check only in the largest unrolling
          if (i == terminationState.getNumberOfIterationsAtLoopHead(locationState) - 2
              && checkFixpoint(interpolant, terminationState.getPossibleTransitionInvariant())) {
            Optional.of(result.withAction(Action.BREAK));
          }
          terminationState.widenPossibleTransitionInvariant(
              bfmgr.or(
                  interpolant,
                  terminationState.getPossibleTransitionInvariant()));
        }
      }
    }
    return Optional.of(result);
  }

  private boolean checkFixpoint(
      BooleanFormula pInterpolant,
      BooleanFormula pPossibleTransitionInvariant) {
    try {
      return solver.implies(pPossibleTransitionInvariant, pInterpolant);
    } catch(Exception e) {
      return false;
    }
  }

  private boolean programContainsPointers(SSAMap pSSAMap) {
    for (String variable : pSSAMap.allVariables()) {
      if (pSSAMap.getType(variable) instanceof CPointerType) {
        return true;
      }
    }
    return false;
  }

  private ImmutableList<BooleanFormula> buildCycleFormula(
      List<BooleanFormula> pFullPathFormula,
      List<BooleanFormula> storedValues,
      SSAMap pSSAMap,
      int pSSAIndex) {
    BooleanFormula prefix = bfmgr.and(pFullPathFormula.subList(0, pSSAIndex+1));
    BooleanFormula cycle = bfmgr.and(pFullPathFormula.subList(pSSAIndex, pFullPathFormula.size()));
    Set<BooleanFormula> extendedFormula = new HashSet<>();
    BooleanFormula newAssignment;

    for (String variable : pSSAMap.allVariables()) {
      String newVariable = "__Q__" + variable;
      newAssignment =
          fmgr.makeEqual(
              fmgr.makeVariable(
                  ctoFormulaConverter.getFormulaTypeFromCType(pSSAMap.getType(variable)),
                  newVariable,
                  pSSAIndex),
              fmgr.makeVariable(
                  ctoFormulaConverter.getFormulaTypeFromCType(pSSAMap.getType(variable)),
                  variable,
                  pSSAMap.getIndex(variable)));
      extendedFormula.add(newAssignment);
    }
    return ImmutableList.of(bfmgr.and(prefix, storedValues.get(pSSAIndex), cycle), bfmgr.and(extendedFormula));
  }
}
