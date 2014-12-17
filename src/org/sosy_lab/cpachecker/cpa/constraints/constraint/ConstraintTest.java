/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicValueFactory;

/**
 * Test class for {@link org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint}
 */
public class ConstraintTest {

  private final static long NUMBER = 5l;

  private final static NumericValue DEFAULT_NUM_VAL = new NumericValue(NUMBER);
  private final static NumericValue GREATER_NUM_VAL = new NumericValue(NUMBER + 1000);

  private final static SymbolicValue SYMBOLIC_ID =
      SymbolicValueFactory.getInstance().createIdentifier(JSimpleType.getInt());
  private final static SymbolicValue OTHER_SYMBOLIC_ID =
      SymbolicValueFactory.getInstance().createIdentifier(JSimpleType.getInt());

  private Constraint constraint;
/*
  @Before
  public void setUp() {
    constraint = new Constraint(DEFAULT_NUM_VAL, Constraint.Operator.LESS_EQUAL, SYMBOLIC_ID);
  }

  @Test
  public void includes() {
    Constraint otherConstraint = new Constraint(DEFAULT_NUM_VAL, Constraint.Operator.LESS, SYMBOLIC_ID);

    Assert.assertFalse(otherConstraint.includes(constraint));
    Assert.assertTrue(constraint.includes(otherConstraint));

    otherConstraint = new Constraint(DEFAULT_NUM_VAL, Constraint.Operator.LESS_EQUAL, SYMBOLIC_ID);
    Assert.assertTrue(constraint.includes(otherConstraint));

    otherConstraint = new Constraint(GREATER_NUM_VAL, Constraint.Operator.EQUAL, SYMBOLIC_ID);
    Assert.assertTrue(constraint.includes(otherConstraint));

    otherConstraint = new Constraint(SYMBOLIC_ID, Constraint.Operator.EQUAL, GREATER_NUM_VAL);
    Assert.assertTrue(constraint.includes(otherConstraint));
  }
  */
}
