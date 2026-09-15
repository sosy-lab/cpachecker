// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Unit tests for {@link SymbolicValues}. */
public class SymbolicValuesTest {

  private final MemoryLocation memLoc1 = MemoryLocation.forIdentifier("a");
  private final MemoryLocation memLoc2 = MemoryLocation.forIdentifier("b");

  private final Type defType = CNumericTypes.INT;

  private final SymbolicIdentifier id1 = SymbolicValueFactory.getInstance().newIdentifier(memLoc1);
  private final SymbolicIdentifier id2 = SymbolicValueFactory.getInstance().newIdentifier(memLoc2);

  private final SymbolicExpression idExp1 = ConstantSymbolicExpression.of(id1, defType);
  private final SymbolicExpression idExp2 = ConstantSymbolicExpression.of(id2, defType);

  private final NumericValue num1 = new NumericValue(5);
  private final SymbolicExpression numExp1 = ConstantSymbolicExpression.of(num1, defType);

  @Test
  public void testRepresentSameCCodeExpression_diffValueSameLocation() {
    final SymbolicExpression locLessExp1 = AdditionExpression.of(idExp1, idExp2, defType, defType);
    final SymbolicExpression locLessExp2 = AdditionExpression.of(idExp1, numExp1, defType, defType);

    SymbolicExpression exp1 = locLessExp1.copyForLocation(memLoc1);
    SymbolicExpression exp2 = locLessExp1.copyForLocation(memLoc2);
    SymbolicExpression exp3 = locLessExp2.copyForLocation(memLoc2);

    SymbolicExpression constr1 = LessThanExpression.of(exp1, exp3, defType, defType);
    SymbolicExpression constr2 = LessThanExpression.of(exp1, exp2, defType, defType);

    assertThat(SymbolicValues.representSameCCodeExpression(exp1, exp1)).isTrue();
    assertThat(SymbolicValues.representSameCCodeExpression(constr1, constr2)).isTrue();
    assertThat(SymbolicValues.representSameCCodeExpression(exp1, exp2)).isFalse();
  }

  @Test
  public void testRepresentSameCCodeExpression_constraintAndItsNegation() {
    final SymbolicExpression expWithLocation1 = idExp1.copyForLocation(memLoc1);
    final SymbolicExpression expWithLocation2 = idExp2.copyForLocation(memLoc2);

    SymbolicExpression constraint =
        AdditionExpression.of(expWithLocation1, numExp1, defType, defType);
    constraint = LessThanExpression.of(constraint, expWithLocation2, defType, defType);
    final SymbolicExpression negation = NegationExpression.of(constraint, defType);

    assertThat(SymbolicValues.representSameCCodeExpression(constraint, negation)).isFalse();
  }
}
