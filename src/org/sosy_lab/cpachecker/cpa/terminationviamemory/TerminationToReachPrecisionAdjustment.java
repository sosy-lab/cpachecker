// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.base.Function;
import java.util.logging.Logger;
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
  public TerminationToReachPrecisionAdjustment(Solver pSolver,
                                               BooleanFormulaManagerView pBfmgr,
                                               FormulaManagerView pFmgr) {
    solver = pSolver;
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
        for (int i = 0; i < terminationState.getNumberOfIterationsAtLoopHead(locationState); ++i) {
          for (String variable : ssaMap.allVariables()) {
            boolean isTargetStateReachable = false;
            try (ProverEnvironment bmcProver = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
              Formula targetFormula = buildCycleFormula(variable,
                  predicateState,
                  (BooleanFormula) terminationState.getStoredValues().get(locationState),
                  ssaMap,
                  ssaMap.getIndex(variable));
              try {
                bmcProver.push((BooleanFormula) targetFormula);
                isTargetStateReachable = !bmcProver.isUnsat();
              } catch (SolverException e){
                continue;
              }
              if (isTargetStateReachable) {
                result = result.withAction(Action.BREAK);
              }
            }
          }
        }
      }
    }

    return Optional.of(result);
  }

  private Formula buildCycleFormula(String pVariable,
                                           PredicateAbstractState pPredicateState,
                                           BooleanFormula storedValues,
                                           SSAMap pSSAMap,
                                           int pSSAIndex) {
    BooleanFormula cycle = pPredicateState.getPathFormula().getFormula();
    cycle = bfmgr.and(cycle, storedValues);
    String newVariable = "__Q__" + pVariable;
    BooleanFormula extendedFormula;
    extendedFormula = fmgr.assignment(fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(18),
                newVariable, pSSAIndex),
            fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(18),
                pVariable, pSSAMap.getIndex(pVariable)));
    cycle = fmgr.makeAnd(cycle, extendedFormula);
    return cycle;
  }
}
