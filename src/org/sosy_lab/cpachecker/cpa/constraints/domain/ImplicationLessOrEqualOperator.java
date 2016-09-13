/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.constraints.domain;

import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Less-or-equal operator for
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA} that defines
 * less-or-equal as <code>c less or equal c' iff </code>
 */
public class ImplicationLessOrEqualOperator implements AbstractDomain {

  private final Solver solver;

  public ImplicationLessOrEqualOperator(final Solver pSolver) {
    solver = pSolver;
  }

  @Override
  public boolean isLessOrEqual(
      final AbstractState pLesserState,
      final AbstractState pBiggerState
  ) throws UnrecognizedCCodeException, InterruptedException, CPAException {

    assert pLesserState instanceof ConstraintsState;
    assert pBiggerState instanceof ConstraintsState;

    ConstraintsState lesserState = (ConstraintsState) pLesserState;
    ConstraintsState biggerState = (ConstraintsState) pBiggerState;

    // if the bigger state is not yet initialized, it has to be empty to be bigger than the lesser
    // state. if the lesser is not yet initialized, it must be empty and as such the bigger state
    // has to be empty too, to be bigger.
    if (!biggerState.isInitialized() || !lesserState.isInitialized()) {
      assert lesserState.isInitialized() || lesserState.isEmpty();
      return biggerState.isEmpty();
    }

    IdentifierAssignment lesserStateDefinites = lesserState.getDefiniteAssignment();
    IdentifierAssignment biggerStateDefinites = biggerState.getDefiniteAssignment();

    try {
      return containsAll(lesserStateDefinites, biggerStateDefinites)
          && implies(lesserState, biggerState);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure", e);
    }

  }

  private boolean implies(ConstraintsState pLesserState, ConstraintsState pBiggerState)
      throws SolverException, InterruptedException, UnrecognizedCCodeException {
    BooleanFormula implyingFormula = pLesserState.getFullFormula();
    BooleanFormula impliedFormula = pBiggerState.getFullFormula();

    return solver.implies(implyingFormula, impliedFormula);
  }

  private boolean containsAll(IdentifierAssignment pLesserStateDefinites,
      IdentifierAssignment pBiggerStateDefinites) {

    if (pBiggerStateDefinites.size() > pLesserStateDefinites.size()) {
      return false;
    }

    for (Map.Entry<SymbolicIdentifier, Value> e : pBiggerStateDefinites.entrySet()) {
      if (pLesserStateDefinites.containsKey(e.getKey())
          && pLesserStateDefinites.get(e.getKey()).equals(e.getValue())) {

        continue;
      } else {
        return false;
      }
    }

    return true;
  }

  @Override
  public AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {

    throw new UnsupportedOperationException("ConstraintsCPA's domain does not support join");
  }
}
