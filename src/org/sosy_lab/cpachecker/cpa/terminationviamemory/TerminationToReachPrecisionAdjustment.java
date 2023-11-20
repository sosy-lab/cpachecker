// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
  private final FormulaManagerView predFmgr;
  private final CtoFormulaConverter ctoFormulaConverter;
  private final TerminationToReachStatistics statistics;
  private final CFA cfa;

  public TerminationToReachPrecisionAdjustment(
      Solver pSolver,
      TerminationToReachStatistics pStatistics,
      CFA pCFA,
      BooleanFormulaManagerView pBfmgr,
      FormulaManagerView pFmgr,
      FormulaManagerView pPredFmgr,
      CToFormulaConverterWithPointerAliasing pCtoFormulaConverter) {
    solver = pSolver;
    statistics = pStatistics;
    cfa = pCFA;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    predFmgr = pPredFmgr;
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
      terminationState.putNewPathFormula(
          fmgr.translateFrom(predicateState.getPathFormula().getFormula(), predFmgr));

      for (int i = 0;
          i < terminationState.getNumberOfIterationsAtLoopHead(locationState) - 1;
          ++i) {
        boolean isTargetStateReachable = false;
        BooleanFormula targetFormula =
            buildCycleFormula(
                buildFullPathFormula(terminationState.getPathFormulas()),
                terminationState.getStoredValues().get(locationState),
                ssaMap,
                i);
        try {
          isTargetStateReachable = !solver.isUnsat(targetFormula);
        } catch (SolverException e) {
          continue;
        }
        if (isTargetStateReachable) {
          terminationState.makeTarget();
          result = result.withAbstractState(terminationState);
          statistics.setNonterminatingLoop(
              cfa.getLoopStructure().orElseThrow().getLoopsForLoopHead(location));
          return Optional.of(result.withAction(Action.BREAK));
        }
      }
    }
    return Optional.of(result);
  }

  private BooleanFormula buildCycleFormula(
      BooleanFormula pFullPathFormula,
      List<BooleanFormula> storedValues,
      SSAMap pSSAMap,
      int pSSAIndex) {
    BooleanFormula cycle = pFullPathFormula;
    BooleanFormula extendedFormula;

    cycle = bfmgr.and(cycle, storedValues.get(pSSAIndex));
    for (String variable : pSSAMap.allVariables()) {
      String newVariable = "__Q__" + variable;
      extendedFormula =
          fmgr.assignment(
              fmgr.makeVariable(
                  ctoFormulaConverter.getFormulaTypeFromCType(pSSAMap.getType(variable)),
                  newVariable,
                  pSSAIndex),
              fmgr.makeVariable(
                  ctoFormulaConverter.getFormulaTypeFromCType(pSSAMap.getType(variable)),
                  variable,
                  pSSAMap.getIndex(variable)));
      cycle = bfmgr.and(cycle, extendedFormula);
    }
    return cycle;
  }

  private BooleanFormula buildFullPathFormula(Set<BooleanFormula> pPathFormulas) {
    List<BooleanFormula> formulas = new ArrayList<>(pPathFormulas);
    return bfmgr.and(formulas);
  }
}
