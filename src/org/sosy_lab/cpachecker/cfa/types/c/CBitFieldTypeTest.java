/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.types.c;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.ClassSanityTester;
import com.google.common.testing.SerializableTester;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;

@RunWith(value = BlockJUnit4ClassRunner.class)
public class CBitFieldTypeTest {

  private final ClassSanityTester tester = new ClassSanityTester();

  public CBitFieldTypeTest() {
    tester.setDefault(CType.class, CNumericTypes.INT);
  }

  @Test
  public void testNulls() throws Exception {
    tester.testNulls(CBitFieldType.class);
  }

  @Test
  public void testEquals() throws Exception {
    tester.testEquals(CBitFieldType.class);
  }

  @Test
  public void testSerializable() throws Exception {
    SerializableTester.reserializeAndAssert(new CBitFieldType(CNumericTypes.INT, 8));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBaseTypeVoid() {
    new CBitFieldType(CVoidType.VOID, 0);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBaseTypeFloat() {
    new CBitFieldType(CNumericTypes.FLOAT, 0);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBaseTypeDouble() {
    new CBitFieldType(CNumericTypes.DOUBLE, 0);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBaseTypeProblem() {
    new CBitFieldType(new CProblemType("Problem"), 0);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBaseTypePointer() {
    new CBitFieldType(new CPointerType(false, false, CNumericTypes.INT), 0);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBaseTypeElaborateStruct() {
    new CBitFieldType(
        new CElaboratedType(false, false, ComplexTypeKind.STRUCT, "DummyElaborateStruct", "DummyElaborateStruct", null), 0);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testNegativeSize() {
    new CBitFieldType(CNumericTypes.INT, -1);
  }

  @Test
  public void testBaseBool() {
    CBitFieldType type = new CBitFieldType(CNumericTypes.BOOL, 0);
    Assert.assertEquals(CNumericTypes.BOOL, type.getType());
    Assert.assertEquals(0, type.getBitFieldSize());
  }

  @Test
  public void testBaseChar() {
    CBitFieldType type = new CBitFieldType(CNumericTypes.CHAR, 0);
    Assert.assertEquals(CNumericTypes.CHAR, type.getType());
    Assert.assertEquals(0, type.getBitFieldSize());
  }

  @Test
  public void testBaseInt() {
    CBitFieldType type = new CBitFieldType(CNumericTypes.INT, 0);
    Assert.assertEquals(CNumericTypes.INT, type.getType());
    Assert.assertEquals(0, type.getBitFieldSize());
  }

  @Test
  public void testBasePositiveSize() {
    CBitFieldType type = new CBitFieldType(CNumericTypes.INT, 3);
    Assert.assertEquals(CNumericTypes.INT, type.getType());
    Assert.assertEquals(3, type.getBitFieldSize());
  }

  @Test
  public void testBaseTypeEnum() {
    new CBitFieldType(new CEnumType(false, false, ImmutableList.of(), "DummyEnum", "DummyEnum"), 0);
  }

  @Test
  public void testBaseTypeElaborateEnum() {
    new CBitFieldType(
        new CElaboratedType(false, false, ComplexTypeKind.ENUM, "DummyElaborateEnum", "DummyElaborateEnum", null), 0);
  }

}
