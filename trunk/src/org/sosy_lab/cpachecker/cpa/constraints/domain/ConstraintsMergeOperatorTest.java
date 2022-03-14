// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.domain;

import static com.google.common.truth.Truth.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Unit tests for {@link ConstraintsMergeOperator} */
public class ConstraintsMergeOperatorTest {

  private final ConstraintsMergeOperator op =
      new ConstraintsMergeOperator(new ConstraintsStatistics());
  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
  private final Type defType = CNumericTypes.INT;

  private final MemoryLocation memLoc1 = MemoryLocation.forIdentifier("id1");
  private final SymbolicExpression idExp1 =
      factory.asConstant(factory.newIdentifier(memLoc1), defType);
  private final SymbolicExpression numExp1 = factory.asConstant(new NumericValue(1), defType);

  private final Constraint posConst = factory.equal(idExp1, numExp1, defType, defType);
  private final Constraint negConst =
      (Constraint) factory.notEqual(idExp1, numExp1, defType, defType);

  @Test
  public void testMerge_mergePossible() throws Exception {
    Set<Constraint> constraints = getConstraints();

    ConstraintsState state1 = new ConstraintsState(constraints);
    state1.add(posConst);

    constraints = getConstraints();

    ConstraintsState state2 = new ConstraintsState(constraints);
    state2.add(negConst);

    ConstraintsState mergeResult =
        (ConstraintsState) op.merge(state1, state2, SingletonPrecision.getInstance());

    assertThat(mergeResult).hasSize(state2.size() - 1);
    assertThat(mergeResult).doesNotContain(negConst);

    state2.remove(negConst);
    assertThat(mergeResult).isEqualTo(state2);
  }

  private Set<Constraint> getConstraints() {
    Set<Constraint> constraints = new HashSet<>();

    // this results in a new symbolic identifier at every method call
    SymbolicExpression idExp2 = factory.asConstant(factory.newIdentifier(memLoc1), defType);

    Constraint currConstr = (Constraint) factory.greaterThan(idExp2, numExp1, defType, defType);
    constraints.add(currConstr);

    currConstr =
        (Constraint)
            factory.logicalNot(factory.lessThanOrEqual(idExp2, numExp1, defType, defType), defType);

    constraints.add(currConstr);

    return constraints;
  }
}
