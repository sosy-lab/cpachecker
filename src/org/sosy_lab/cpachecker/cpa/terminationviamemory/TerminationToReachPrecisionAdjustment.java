// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.base.Function;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
  private final TerminationToReachStatistics statistics;
  private final CFA cfa;

  public TerminationToReachPrecisionAdjustment(
      Solver pSolver,
      TerminationToReachStatistics pStatistics,
      CFA pCFA,
      BooleanFormulaManagerView pBfmgr,
      FormulaManagerView pFmgr) {
    solver = pSolver;
    statistics = pStatistics;
    cfa = pCFA;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
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
        BooleanFormula targetFormula =
            buildCycleFormula(
                buildFullPathFormula(terminationState.getPathFormulas()),
                terminationState.getStoredValues().get(locationState),
                i);
        try {
          isTargetStateReachable = !solver.isUnsat(targetFormula);
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
        }
      }
    }
    return Optional.of(result);
  }

  private boolean programContainsPointers(SSAMap pSSAMap) {
    for (String variable : pSSAMap.allVariables()) {
      if (pSSAMap.getType(variable) instanceof CPointerType) {
        return true;
      }
    }
    return false;
  }

  private BooleanFormula buildCycleFormula(
      BooleanFormula pFullPathFormula, List<BooleanFormula> storedValues, int pSSAIndex) {
    BooleanFormula cycle = pFullPathFormula;
    BooleanFormula extendedFormula;
    Map<String, Formula> mapNamesToFormulas = fmgr.extractVariables(pFullPathFormula);

    cycle = bfmgr.and(cycle, storedValues.get(pSSAIndex));
    for (Entry<String, Formula> variable : mapNamesToFormulas.entrySet()) {
      String newVariable =
          "__Q__"
              + fmgr.uninstantiate(variable.getValue()).toString().replace("@", "");
      extendedFormula =
          fmgr.assignment(
              fmgr.makeVariable(
                  fmgr.getFormulaType(variable.getValue()), newVariable, pSSAIndex),
              variable.getValue());
      cycle = bfmgr.and(cycle, extendedFormula);
    }
    return cycle;
  }

  private BooleanFormula buildFullPathFormula(List<BooleanFormula> pPathFormulas) {
    return bfmgr.and(pPathFormulas);
  }
}
