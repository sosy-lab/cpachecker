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
package org.sosy_lab.cpachecker.cfa.types;

import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.OptionalInt;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;

@RunWith(Parameterized.class)
@SuppressFBWarnings(
  value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
  justification = "Fields are filled by parameterization of JUnit"
)
public class MachineModelSizeOfVisitorTest {

  private static final String TEST_STRUCT = "testStruct";

  private static final String FIRST_BITFIELD_12 = "firstBitfield";
  private static final String SECOND_BITFIELD_10 = "secondBitfield";
  private static final String LONGLONG = "longlong";
  private static final String THIRD_INT = "thirdInt";
  private static final String CHAR = "char";
  private static final String INT_POINTER = "int_pointer";
  private static final String VOID_POINTER = "void_pointer";
  private static final String LONGLONG_BITFIELD = "longlong_bitfield";
  private static final String CHAR_BITFIELD_5 = "char_bitfield_5";
  private static final String CHAR_BITFIELD_4_1 = "char_bitfield_4_1";
  private static final String CHAR_BITFIELD_4_0 = "char_bitfield_4_0";
  private static final String LONG_BITFIELD_0 = "long_bitfield_0";
  private static final String LONG_BITFIELD_9 = "long_bitfield_9";
  private static final String LONG_BITFIELD_15 = "long_bitfield_15";
  private static final String LONG_BITFIELD_18 = "long_bitfield_18";

  private static final MachineModel MODEL64 = MachineModel.LINUX64;
  private static final MachineModel MODEL32 = MachineModel.LINUX32;

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.UNSIGNED_INT, 12), FIRST_BITFIELD_12),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.UNSIGNED_INT, 10), SECOND_BITFIELD_10),
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT));

  // struct s { unsigned int a : 12; unsigned int b : 10; int c; };
  private static final CCompositeType STRUCT_1 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_2 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.CHAR, 4), CHAR_BITFIELD_4_0),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.CHAR, 4), CHAR_BITFIELD_4_1),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.UNSIGNED_INT, 12), FIRST_BITFIELD_12),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.UNSIGNED_INT, 10), SECOND_BITFIELD_10),
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT));

  // struct s { char a : 4; char b : 4; unsigned int c : 12; unsigned int d : 10; int e; };
  private static final CCompositeType STRUCT_2 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS_2, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_3 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.CHAR, 4), CHAR_BITFIELD_4_0),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.CHAR, 5), CHAR_BITFIELD_5),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.UNSIGNED_INT, 12), FIRST_BITFIELD_12),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.UNSIGNED_INT, 10), SECOND_BITFIELD_10),
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT));

  // struct s { char a : 4; char b : 5; unsigned int c : 12; unsigned int d : 10; int e; };
  private static final CCompositeType STRUCT_3 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS_3, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_4 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_LONG_INT, 8), LONGLONG_BITFIELD));

  // struct s { long long a : 8; };
  private static final CCompositeType STRUCT_4 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS_4, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_5 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(CNumericTypes.CHAR, CHAR),
          new CCompositeTypeMemberDeclaration(CNumericTypes.LONG_LONG_INT, LONGLONG),
          new CCompositeTypeMemberDeclaration(
              new CPointerType(false, false, CNumericTypes.INT), INT_POINTER),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.CHAR, 5), CHAR_BITFIELD_5),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.CHAR, 4), CHAR_BITFIELD_4_0),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.INT, 12), FIRST_BITFIELD_12),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_LONG_INT, 3), LONGLONG_BITFIELD),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.INT, 2), SECOND_BITFIELD_10),
          new CCompositeTypeMemberDeclaration(CPointerType.POINTER_TO_VOID, VOID_POINTER));

  // struct s { char a; long long b; int* c; char d : 5; char e : 4; int f : 12; long long g : 3;
  // int h : 2; void* i; };
  private static final CCompositeType STRUCT_5 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS_5, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_6 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 15), LONG_BITFIELD_15),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 0), LONG_BITFIELD_0));

  //  struct s { int a; long b : 15; long : 0; };
  private static final CCompositeType STRUCT_6 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS_6, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_7 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 15), LONG_BITFIELD_15),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 18), LONG_BITFIELD_18),
          new CCompositeTypeMemberDeclaration(CNumericTypes.CHAR, CHAR));

  // struct s { int a; long b : 15; long c : 18; char d; };
  private static final CCompositeType STRUCT_7 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS_7, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_8 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 15), LONG_BITFIELD_15),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 9), LONG_BITFIELD_9),
          new CCompositeTypeMemberDeclaration(CNumericTypes.CHAR, CHAR));

  // struct s { int a; long b : 15; long c : 9; char d; };
  private static final CCompositeType STRUCT_8 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS_8, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_9 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 15), LONG_BITFIELD_15),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 0), LONG_BITFIELD_0),
          new CCompositeTypeMemberDeclaration(CNumericTypes.CHAR, CHAR));

  // struct s { int a; long b : 15; long : 0; char d; };
  private static final CCompositeType STRUCT_9 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS_9, TEST_STRUCT, TEST_STRUCT);

  @Parameters(name = "{3}: {1}")
  public static Object[][] machineModels() {
    Object[][] types =
        new Object[][] {
          // struct   // machinemodel // expected size            // name
          // according to gcc, c11
          {STRUCT_1, MODEL64, 8, "Struct_1"},
          {STRUCT_1, MODEL32, 8, "Struct_1"},
          {STRUCT_2, MODEL64, 8, "Struct_2"},
          {STRUCT_2, MODEL32, 8, "Struct_2"},
          {STRUCT_3, MODEL64, 12, "Struct_3"},
          {STRUCT_3, MODEL32, 12, "Struct_3"},
          {STRUCT_4, MODEL64, 8, "Struct_4"},
          {STRUCT_4, MODEL32, 4, "Struct_4"},
          {STRUCT_5, MODEL64, 40, "Struct_5"},
          {STRUCT_5, MODEL32, 24, "Struct_5"},
          {STRUCT_6, MODEL64, 8, "Struct_6"},
          {STRUCT_6, MODEL32, 8, "Struct_6"},
          {STRUCT_7, MODEL64, 16, "Struct_7"},
          {STRUCT_7, MODEL32, 12, "Struct_7"},
          {STRUCT_8, MODEL64, 8, "Struct_8"},
          {STRUCT_8, MODEL32, 8, "Struct_8"},
          {STRUCT_9, MODEL64, 16, "Struct_9"},
          {STRUCT_9, MODEL32, 12, "Struct_9"}
        };

    return types;
  }

  @Parameter(0)
  public CCompositeType testStruct;

  @Parameter(1)
  public MachineModel model;

  @Parameter(2)
  public int expectedSize;

  @Parameter(3)
  public String name;

  @Test
  public void testSizeOfStruct() {
    assertThat(OptionalInt.of(model.getSizeof(testStruct))).hasValue(expectedSize);
  }
}
