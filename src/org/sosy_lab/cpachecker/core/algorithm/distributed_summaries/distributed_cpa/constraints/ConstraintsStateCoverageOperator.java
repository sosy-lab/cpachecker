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

public class ConstraintsStateCoverageOperator implements CoverageOperator {
  public final ConstraintsSolver solver;
  public final String functionName;

  public ConstraintsStateCoverageOperator(ConstraintsCPA pCPA, CFANode pCFANode) {
    solver = pCPA.getSolver();
    functionName = pCFANode.getFunctionName();
  }

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {

    Set<Constraint> constraints1 = new HashSet<>((ConstraintsState) state1);
    Set<Constraint> constraints2 = new HashSet<>((ConstraintsState) state2);

    if (constraints2.isEmpty()) return true;
    BooleanFormulaManagerView bfm = solver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula stateAsFormula1 = bfm.and(solver.getFullFormula(constraints1, functionName));
    BooleanFormula stateAsFormula2 = bfm.and(solver.getFullFormula(constraints2, functionName));

    return bfm.isTrue(bfm.implication(stateAsFormula1, stateAsFormula2));
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
