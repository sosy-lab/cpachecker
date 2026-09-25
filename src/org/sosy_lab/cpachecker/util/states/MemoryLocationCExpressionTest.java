// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.states;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

/**
 * Tests that the offset of a {@link MemoryLocation}, which counts bytes, is converted to and from
 * the C expression denoting the same address, where an index counts elements.
 */
public class MemoryLocationCExpressionTest {

  private static final MachineModel MACHINE_MODEL = MachineModel.LINUX32;

  // int[4]
  private static final CType INT_ARRAY =
      new CArrayType(
          CTypeQualifiers.NONE,
          CNumericTypes.INT,
          CIntegerLiteralExpression.createDummyLiteral(4, CNumericTypes.INT));

  // struct point { int x; int y; }
  private static final CCompositeType POINT =
      new CCompositeType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(
              new CCompositeTypeMemberDeclaration(CNumericTypes.INT, "x"),
              new CCompositeTypeMemberDeclaration(CNumericTypes.INT, "y")),
          "point",
          "point");

  // struct line { struct point points[2]; int label; }
  private static final CCompositeType LINE =
      new CCompositeType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(
              new CCompositeTypeMemberDeclaration(
                  new CArrayType(
                      CTypeQualifiers.NONE,
                      POINT,
                      CIntegerLiteralExpression.createDummyLiteral(2, CNumericTypes.INT)),
                  "points"),
              new CCompositeTypeMemberDeclaration(CNumericTypes.INT, "label")),
          "line",
          "line");

  private static void assertRoundTrip(String pExpected, MemoryLocation pLocation, CType pType) {
    assertThat(pLocation.asCExpression(pType, MACHINE_MODEL)).hasValue(pExpected);
    assertThat(
            MemoryLocation.parseCExpression(pExpected, Optional.of("main"), pType, MACHINE_MODEL))
        .hasValue(pLocation);
  }

  @Test
  public void variableWithoutOffset() {
    assertRoundTrip("&x", MemoryLocation.forLocalVariable("main", "x"), CNumericTypes.INT);
  }

  @Test
  public void arrayIndexCountsElementsAndNotBytes() {
    // An int is 4 bytes wide, so the byte offset 8 is the element with the index 2
    assertRoundTrip("&a[0]", MemoryLocation.forLocalVariable("main", "a", 0), INT_ARRAY);
    assertRoundTrip("&a[2]", MemoryLocation.forLocalVariable("main", "a", 8), INT_ARRAY);
  }

  @Test
  public void structMemberIsNamed() {
    assertRoundTrip("&p.x", MemoryLocation.forLocalVariable("main", "p", 0), POINT);
    assertRoundTrip("&p.y", MemoryLocation.forLocalVariable("main", "p", 4), POINT);
  }

  @Test
  public void nestedArrayOfStructs() {
    assertRoundTrip("&l.points[0].x", MemoryLocation.forLocalVariable("main", "l", 0), LINE);
    assertRoundTrip("&l.points[1].y", MemoryLocation.forLocalVariable("main", "l", 12), LINE);
    assertRoundTrip("&l.label", MemoryLocation.forLocalVariable("main", "l", 16), LINE);
  }

  @Test
  public void offsetInsideAScalarHasNoExpression() {
    // Half way into l.label, which no address expression of the program denotes
    assertThat(MemoryLocation.forLocalVariable("main", "l", 18).asCExpression(LINE, MACHINE_MODEL))
        .isEmpty();
  }

  @Test
  public void offsetOnAScalarHasNoExpression() {
    // '&x' would be read back as the location without an offset, which is a different one
    assertThat(
            MemoryLocation.forLocalVariable("main", "x", 0)
                .asCExpression(CNumericTypes.INT, MACHINE_MODEL))
        .isEmpty();
  }

  @Test
  public void offsetBeyondTheArrayHasNoExpression() {
    assertThat(
            MemoryLocation.forLocalVariable("main", "a", 16)
                .asCExpression(INT_ARRAY, MACHINE_MODEL))
        .isEmpty();
  }

  @Test
  public void expressionNotMatchingTheTypeIsRejected() {
    assertThat(MemoryLocation.parseCExpression("&p.z", Optional.of("main"), POINT, MACHINE_MODEL))
        .isEmpty();
    assertThat(MemoryLocation.parseCExpression("&p[0]", Optional.of("main"), POINT, MACHINE_MODEL))
        .isEmpty();
  }
}
