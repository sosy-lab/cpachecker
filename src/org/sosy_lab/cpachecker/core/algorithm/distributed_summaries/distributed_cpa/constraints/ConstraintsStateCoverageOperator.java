// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver.SolverResult;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

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

    ImmutableSet<Constraint> constraints1 = ((ConstraintsState) state1).getConstraints();
    ImmutableSet<Constraint> constraints2 = ((ConstraintsState) state2).getConstraints();

    HashSet<Constraint> constraints = HashSet.newHashSet(constraints1.size() + constraints2.size());

    constraints.addAll(constraints1);
    for (Constraint c : constraints2) {
      constraints.add(new LogicalNotExpression((SymbolicExpression) c, c.getType()));
    }

    try {
      SolverResult result =
          solver.checkUnsatWithFreshSolver(new ConstraintsState(constraints), functionName);
      return result.isUNSAT();
    } catch (SolverException pE) {
      throw new CPAException("Solver failed checking ConstraintState subsumption", pE);
    }
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
