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

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;


/**
 * Unit tests for {@link ConstraintsMergeOperator}
 */
public class ConstraintsMergeOperatorTest {

  private final ConstraintsMergeOperator op = new ConstraintsMergeOperator();
  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
  private final Type defType = CNumericTypes.INT;

  private final SymbolicExpression idExp1 = factory.asConstant(factory.newIdentifier(), defType);
  private final SymbolicExpression numExp1 = factory.asConstant(new NumericValue(1), defType);

  private final Constraint posConst = factory.equal(idExp1, numExp1, defType, defType);
  private final Constraint negConst = (Constraint) factory.notEqual(idExp1, numExp1, defType, defType);

  @Test
  public void testMerge_mergePossible() throws Exception {
    Set<Constraint> constraints = getConstraints();

    ConstraintsState state1 = new ConstraintsState(constraints, new IdentifierAssignment());
    state1.add(posConst);

    constraints = getConstraints();

    ConstraintsState state2 = new ConstraintsState(constraints, new IdentifierAssignment());
    state2.add(negConst);

    ConstraintsState mergeResult = (ConstraintsState) op.merge(state1, state2, SingletonPrecision.getInstance());

    Assert.assertTrue(mergeResult.size() + 1 == state2.size());
    Assert.assertTrue(!mergeResult.contains(negConst));

    state2.remove(negConst);
    Assert.assertEquals(state2, mergeResult);
  }

  private Set<Constraint> getConstraints() {
    Set<Constraint> constraints = new HashSet<>();

    // this results in a new symbolic identifier at every method call
    SymbolicExpression idExp2 = factory.asConstant(factory.newIdentifier(), defType);

    Constraint currConstr = (Constraint) factory.greaterThan(idExp2, numExp1, defType, defType);
    constraints.add(currConstr);

    currConstr = (Constraint)
        factory.logicalNot(factory.lessThanOrEqual(idExp2, numExp1, defType, defType), defType);

    constraints.add(currConstr);

    return constraints;
  }
}