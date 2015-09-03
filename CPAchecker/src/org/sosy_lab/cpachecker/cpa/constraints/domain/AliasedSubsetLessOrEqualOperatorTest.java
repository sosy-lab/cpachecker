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
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA.ComparisonType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;

import com.google.common.collect.Iterables;

/**
 * Tests for {@link AliasedSubsetLessOrEqualOperator}.
 */
public class AliasedSubsetLessOrEqualOperatorTest {

  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
  private final Type defType = CNumericTypes.INT;

  private final AliasedSubsetLessOrEqualOperator
      leqOp = AliasedSubsetLessOrEqualOperator.getInstance();

  private final IdentifierAssignment emptyDefiniteAssignment = new IdentifierAssignment();

  private final SymbolicIdentifier id1 = factory.newIdentifier();
  private final SymbolicIdentifier id2 = factory.newIdentifier();
  private final SymbolicIdentifier alias1 = factory.newIdentifier();
  private final SymbolicIdentifier alias2 = factory.newIdentifier();

  private final SymbolicExpression idExp1 = factory.asConstant(id1,
      defType);
  private final SymbolicExpression idExp2 = factory.asConstant(id2,
      defType);
  private final SymbolicExpression aliasExp1 = factory.asConstant(alias1,
      defType);
  private final SymbolicExpression aliasExp2 = factory.asConstant(alias2,
      defType);

  private final NumericValue num1 = new NumericValue(5);
  private final SymbolicExpression numExp1 = factory.asConstant(num1, defType);

  private final Constraint c1 = (Constraint) factory.lessThan(idExp1, numExp1, defType, defType);
  private final Constraint c2 = factory.equal(idExp2, idExp1, defType, defType);
  private final Constraint cAlias1 = (Constraint) factory.lessThan(aliasExp1, numExp1, defType, defType);
  private final Constraint cAlias2 = factory.equal(aliasExp2, aliasExp1, defType, defType);

  public AliasedSubsetLessOrEqualOperatorTest() {
    SymbolicValues.initialize(ComparisonType.ALIASED_SUBSET);
  }

  @Test
  public void testIsLessOrEqual_reflexive() {
    org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState
        state = new org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState(getConstraintSet(), emptyDefiniteAssignment);

    Assert.assertTrue(leqOp.isLessOrEqual(state, state));
  }

  @Test
  public void testIsLessOrEqual_BiggerOneEmpty() {
    org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState
        emptyState = new org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState();
    org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState
        state = new org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState(getConstraintSet(), emptyDefiniteAssignment);

    Assert.assertTrue(leqOp.isLessOrEqual(state, emptyState));
  }

  @Test
  public void testIsLessOrEqual_BiggerOneSubsetWithSameIdentifierIds() {
    org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState
        state = new org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState(getConstraintSet(), emptyDefiniteAssignment);
    Set<Constraint> subset = getConstraintSet();
    subset.remove(Iterables.get(subset, 0));
    org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState
        subsetState = new org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState(subset, emptyDefiniteAssignment);

    Assert.assertTrue(leqOp.isLessOrEqual(state, subsetState));
  }

  @Test
  public void testIsLessOrEqual_BiggerOneSubsetWithOtherIdentifierIds() {
    ConstraintsState state = new ConstraintsState(getConstraintSet(), emptyDefiniteAssignment);
    Set<Constraint> subset = getAliasConstraintSet();
    subset.remove(Iterables.get(subset, 0));
    ConstraintsState
        subsetState = new ConstraintsState(subset, emptyDefiniteAssignment);

    Assert.assertTrue(leqOp.isLessOrEqual(state, subsetState));
  }

  private Set<Constraint> getConstraintSet() {

    Set<Constraint> set = new HashSet<>();

    set.add(c1);
    set.add(c2);

    return set;
  }

  private Set<Constraint> getAliasConstraintSet() {
    Set<Constraint> set = new HashSet<>();

    set.add(cAlias1);
    set.add(cAlias2);

    return set;
  }
}