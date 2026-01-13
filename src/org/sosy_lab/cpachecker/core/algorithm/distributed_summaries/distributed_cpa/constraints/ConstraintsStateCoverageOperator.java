// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class ConstraintsStateCoverageOperator implements CoverageOperator {
  private final ConstraintsSolver constraintsSolver;
  private final String functionName;

  public ConstraintsStateCoverageOperator(ConstraintsCPA pCPA, CFANode pCFANode) {
    constraintsSolver = pCPA.getSolver();
    functionName = pCFANode.getFunctionName();
  }

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {

    ValueAnalysisState v1 = AbstractStates.extractStateByType(state1, ValueAnalysisState.class);
    ValueAnalysisState v2 = AbstractStates.extractStateByType(state2, ValueAnalysisState.class);

    ConstraintsState c1 = AbstractStates.extractStateByType(state1, ConstraintsState.class);
    ConstraintsState c2 = AbstractStates.extractStateByType(state2, ConstraintsState.class);

    assert v1 != null && v2 != null && c1 != null && c2 != null;

    if (c2.isEmpty() || c1.equals(c2)) return true;
    BooleanFormulaManagerView bfm =
        constraintsSolver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula stateAsFormula1 = bfm.and(constraintsSolver.getFullFormula(c1, functionName));
    BooleanFormula stateAsFormula2 = bfm.and(constraintsSolver.getFullFormula(c2, functionName));
    BooleanFormula compareValues =
        bfm.and(
            constraintsSolver.getFullFormula(
                ValueAnalysisState.compareInConstraint(v1, v2), functionName));

    try {
      return constraintsSolver
          .getSolver()
          .implies(bfm.and(stateAsFormula1, compareValues), stateAsFormula2);
    } catch (SolverException e) {
      throw new CPAException("Solver encountered an issue when calculating implication.", e);
    }
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
