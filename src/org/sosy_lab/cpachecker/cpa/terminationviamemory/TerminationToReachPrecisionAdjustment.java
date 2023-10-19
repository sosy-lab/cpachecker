// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.base.Function;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.util.globalinfo.SerializationInfoStorage;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public class TerminationToReachPrecisionAdjustment implements PrecisionAdjustment {
  private final Solver solver;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaManagerView fmgr;
  private final FormulaManagerView predFmgr;
  public TerminationToReachPrecisionAdjustment(Solver pSolver,
                                               BooleanFormulaManagerView pBfmgr,
                                               FormulaManagerView pFmgr,
                                               FormulaManagerView pPredFmgr) {
    solver = pSolver;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    predFmgr = pPredFmgr;
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
    LocationState locationState = AbstractStates.extractStateByType(fullState,
        LocationState.class);
    CFANode location = AbstractStates.extractLocation(locationState);
    PredicateAbstractState predicateState = AbstractStates.extractStateByType(fullState,
        PredicateAbstractState.class);

    SSAMap ssaMap = predicateState.getPathFormula().getSsa();
    PrecisionAdjustmentResult result =
        new PrecisionAdjustmentResult(state, precision, Action.CONTINUE);

    if (location.isLoopStart()) {
      if (terminationState.getStoredValues().containsKey(locationState)) {
        for (int i = 0; i < terminationState.getNumberOfIterationsAtLoopHead(locationState) - 1; ++i) {
          boolean isTargetStateReachable = false;
          BooleanFormula targetFormula = buildCycleFormula(predicateState,
              terminationState.getStoredValues().get(locationState),
              ssaMap,
              i);
          try {
              isTargetStateReachable = !solver.isUnsat(targetFormula);
            } catch (SolverException e){
              continue;
            }
            if (isTargetStateReachable) {
              terminationState.makeTarget();
              result = result.withAbstractState(terminationState);
              return Optional.of(result.withAction(Action.BREAK));
            }
          }
        }
      }
    return Optional.of(result);
  }

  private BooleanFormula buildCycleFormula(PredicateAbstractState pPredicateState,
                                           BooleanFormula storedValues,
                                           SSAMap pSSAMap,
                                           int pSSAIndex) {
    BooleanFormula cycle =
        fmgr.translateFrom(
            pPredicateState.getPathFormula().getFormula(),
            predFmgr);
    BooleanFormula extendedFormula;

    cycle = bfmgr.and(cycle, storedValues);
    for (String variable : pSSAMap.allVariables()) {
      String newVariable = "__Q__" + variable;
      extendedFormula =
          fmgr.assignment(
              fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(18), newVariable, pSSAIndex),
              fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(18), variable, pSSAMap.getIndex(variable)));
      cycle = bfmgr.and(cycle, extendedFormula);
    }
    return cycle;
  }
}
