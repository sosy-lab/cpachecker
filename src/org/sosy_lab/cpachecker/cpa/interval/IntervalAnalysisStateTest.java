// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cfa.ast.FileLocation.DUMMY;
import static org.sosy_lab.cpachecker.cpa.interval.funarray.ExpressionUtility.getIntegerExpression;
import static org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder.exp;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cpa.interval.funarray.FunArray;
import org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder;
import org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder.FunArrayBuilderException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

@SuppressWarnings({"unchecked", "rawtypes"})
public class IntervalAnalysisStateTest {

  @Test
  public void pseudoPartiotionKey() {
    IntervalAnalysisState s = new IntervalAnalysisState(null);
    IntervalAnalysisState sa1 = s.addInterval("a", new Interval(1L, 1L), 10, null);
    IntervalAnalysisState sb2 = s.addInterval("b", new Interval(2L, 2L), 10, null);
    IntervalAnalysisState sa1b2 = sa1.addInterval("b", new Interval(2L, 2L), 10, null);
    IntervalAnalysisState sa1b3 = sa1.addInterval("b", new Interval(3L, 3L), 10, null);
    IntervalAnalysisState sa1b23 = sa1.addInterval("b", new Interval(2L, 3L), 10, null);

    Comparable cs = s.getPseudoPartitionKey();
    Comparable csa1 = sa1.getPseudoPartitionKey();
    Comparable csb2 = sb2.getPseudoPartitionKey();
    Comparable csa1b2 = sa1b2.getPseudoPartitionKey();
    Comparable csa1b3 = sa1b3.getPseudoPartitionKey();
    Comparable csa1b23 = sa1b23.getPseudoPartitionKey();

    checkEquals(cs, cs);
    checkEquals(csa1, csa1);
    checkEquals(csb2, csb2);
    checkEquals(csa1b2, csa1b2);
    checkEquals(csa1b3, csa1b3);
    checkEquals(csa1b23, csa1b23);

    checkEquals(csa1, csb2);
    checkEquals(csa1b2, csa1b3);

    checkLess(cs, csa1);
    checkLess(cs, csb2);
    checkLess(cs, csa1b2);
    checkLess(cs, csa1b3);
    checkLess(csa1, csa1b2);
    checkLess(csa1, csa1b3);
    checkLess(csb2, csa1b2);

    checkLess(csa1, csa1b23);
    checkLess(csb2, csa1b23);
    checkLess(csa1b23, csa1b2);
    checkLess(csa1b23, csa1b3);
  }

  private void checkLess(Comparable c1, Comparable c2) {
    assertThat(c1.compareTo(c2) < 0).isTrue();
    assertThat(c2.compareTo(c1) > 0).isTrue();
  }

  private void checkEquals(Comparable c1, Comparable c2) {
    assertThat(c1.compareTo(c2)).isEqualTo(0);
    assertThat(c2.compareTo(c1)).isEqualTo(0);
  }

  @Test
  public void arrayAccessOnUntrackedArrayReturnsUnbound() throws UnrecognizedCodeException {
    IntervalAnalysisState state = new IntervalAnalysisState(null);
    CExpression index = getIntegerExpression(0);
    ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, null);
    Interval result = state.arrayAccess("a", index, visitor);
    assertThat(result).isEqualTo(Interval.UNBOUND);
  }

  @Test
  public void joinMergesArraysFromBothBranches() throws FunArrayBuilder.FunArrayBuilderException {
    FunArray arrayA = FunArrayBuilder.firstBound(exp(0)).value(5, 5).bound(exp("n")).build();
    FunArray arrayB = FunArrayBuilder.firstBound(exp(0)).value(10, 10).bound(exp("n")).build();
    FunArray expected = FunArrayBuilder.firstBound(exp(0)).value(5, 10).bound(exp("n")).build();

    IntervalAnalysisState stateA = new IntervalAnalysisState(null).addArray("a", arrayA, null);
    IntervalAnalysisState stateB = new IntervalAnalysisState(null).addArray("a", arrayB, null);

    IntervalAnalysisState joined = stateA.join(stateB);

    assertThat(joined.arrays().get("a")).isEqualTo(expected);
  }

  @Test
  public void joinPreservesArrayOnlyPresentInOneState()
      throws FunArrayBuilder.FunArrayBuilderException {
    FunArray array = FunArrayBuilder.firstBound(exp(0)).value(3, 3).bound(exp("n")).build();

    IntervalAnalysisState stateWithArray =
        new IntervalAnalysisState(null).addArray("a", array, null);
    IntervalAnalysisState stateWithout = new IntervalAnalysisState(null);

    IntervalAnalysisState joined = stateWithArray.join(stateWithout);

    assertThat(joined.arrays().containsKey("a")).isTrue();
  }

  @Test
  public void testAssignArrayElementUnknownIndexWidensAllSegments()
      throws FunArrayBuilderException {
    FunArray arr = FunArrayBuilder.firstBound(exp(0)).value(5, 5).bound(exp("n")).build();
    IntervalAnalysisState state = new IntervalAnalysisState(null).addArray("a", arr, null);

    state = state.assignArrayElementUnknownIndex("a", new Interval(10L, 10L), null);

    FunArray expected = FunArrayBuilder.firstBound(exp(0)).value(5, 10).bound(exp("n")).build();
    assertThat(state.arrays().get("a")).isEqualTo(expected);
  }

  @Test
  public void arrayAccessWithNonNormalizableIndexReturnsUnbound()
      throws UnrecognizedCodeException, FunArrayBuilderException {
    IntervalAnalysisState state = new IntervalAnalysisState(null);
    FunArray arr =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .mayBeEmpty()
            .bound(exp("n"))
            .build();
    state = state.addArray("a", arr, null);
    CSimpleType intType =
        new CSimpleType(
            CTypeQualifiers.create(false, false, false),
            CBasicType.INT,
            false,
            false,
            true,
            false,
            false,
            false,
            false);
    CBinaryExpression multiplyIndex =
        new CBinaryExpression(
            DUMMY,
            intType,
            intType,
            getIntegerExpression(2),
            getIntegerExpression(3),
            BinaryOperator.MULTIPLY);
    ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, null);
    Interval result = state.arrayAccess("a", multiplyIndex, visitor);
    assertThat(result).isEqualTo(Interval.UNBOUND);
  }
}
