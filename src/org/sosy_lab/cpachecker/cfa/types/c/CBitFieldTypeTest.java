// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.ClassSanityTester;
import com.google.common.testing.SerializableTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;

@RunWith(value = BlockJUnit4ClassRunner.class)
@SuppressWarnings("unused")
public class CBitFieldTypeTest {

  private final ClassSanityTester tester = new ClassSanityTester();

  public CBitFieldTypeTest() {
    tester.setDefault(CType.class, CNumericTypes.INT);
  }

  @Test
  public void testNulls() {
    tester.testNulls(CBitFieldType.class);
  }

  @Test
  public void testEquals() {
    tester.testEquals(CBitFieldType.class);
  }

  @Test
  public void testSerializable() {
    SerializableTester.reserializeAndAssert(new CBitFieldType(CNumericTypes.INT, 8));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBaseTypeVoid() {
    new CBitFieldType(CVoidType.VOID, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBaseTypeFloat() {
    new CBitFieldType(CNumericTypes.FLOAT, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBaseTypeDouble() {
    new CBitFieldType(CNumericTypes.DOUBLE, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBaseTypeProblem() {
    new CBitFieldType(new CProblemType("Problem"), 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBaseTypePointer() {
    new CBitFieldType(new CPointerType(false, false, CNumericTypes.INT), 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBaseTypeElaborateStruct() {
    new CBitFieldType(
        new CElaboratedType(
            false,
            false,
            ComplexTypeKind.STRUCT,
            "DummyElaborateStruct",
            "DummyElaborateStruct",
            null),
        0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeSize() {
    new CBitFieldType(CNumericTypes.INT, -1);
  }

  @Test
  public void testBaseBool() {
    CBitFieldType type = new CBitFieldType(CNumericTypes.BOOL, 0);
    assertThat(type.getType()).isEqualTo(CNumericTypes.BOOL);
    assertThat(type.getBitFieldSize()).isEqualTo(0);
  }

  @Test
  public void testBaseChar() {
    CBitFieldType type = new CBitFieldType(CNumericTypes.CHAR, 0);
    assertThat(type.getType()).isEqualTo(CNumericTypes.CHAR);
    assertThat(type.getBitFieldSize()).isEqualTo(0);
  }

  @Test
  public void testBaseInt() {
    CBitFieldType type = new CBitFieldType(CNumericTypes.INT, 0);
    assertThat(type.getType()).isEqualTo(CNumericTypes.INT);
    assertThat(type.getBitFieldSize()).isEqualTo(0);
  }

  @Test
  public void testBasePositiveSize() {
    CBitFieldType type = new CBitFieldType(CNumericTypes.INT, 3);
    assertThat(type.getType()).isEqualTo(CNumericTypes.INT);
    assertThat(type.getBitFieldSize()).isEqualTo(3);
  }

  @Test
  public void testBaseTypeEnum() {
    new CBitFieldType(new CEnumType(false, false, ImmutableList.of(), "DummyEnum", "DummyEnum"), 0);
  }

  @Test
  public void testBaseTypeElaborateEnum() {
    new CBitFieldType(
        new CElaboratedType(
            false, false, ComplexTypeKind.ENUM, "DummyElaborateEnum", "DummyElaborateEnum", null),
        0);
  }
}
