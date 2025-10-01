// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
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

    Set<Constraint> constraints1 = new HashSet<>((ConstraintsState) state1);
    Set<Constraint> constraints2 = new HashSet<>((ConstraintsState) state2);

    if (constraints2.isEmpty() || constraints1.equals(constraints2)) return true;

    BooleanFormulaManagerView bfm =
        constraintsSolver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula stateAsFormula1 =
        bfm.and(constraintsSolver.getFullFormula(constraints1, functionName));
    BooleanFormula stateAsFormula2 =
        bfm.and(constraintsSolver.getFullFormula(constraints2, functionName));
    try {
      return constraintsSolver.getSolver().implies(stateAsFormula1, stateAsFormula2);
    } catch (SolverException e) {
      throw new CPAException("Solver encountered an issue when calculating implication.", e);
    }
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
