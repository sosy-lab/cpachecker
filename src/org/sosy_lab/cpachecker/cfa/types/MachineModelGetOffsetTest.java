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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

@RunWith(Parameterized.class)
@SuppressFBWarnings(
  value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
  justification = "Fields are filled by parameterization of JUnit"
)
public class MachineModelGetOffsetTest {

  private final static String TEST_STRUCT = "testStruct";

  private final static String FIRST_BITFIELD_12 = "firstBitfield";
  private final static String SECOND_BITFIELD_10 = "secondBitfield";
  private final static String THIRD_INT = "thirdInt";
  private final static String LAST_INCOMPLETEARRAY = "lastIncomplete";
  private static final String LONG_BITFIELD_15 = "long_bitfield_15";
  private static final String LONG_BITFIELD_18 = "long_bitfield_18";
  private static final String CHAR = "char";

  private static final MachineModel MODEL64 = MachineModel.LINUX64;
  private static final MachineModel MODEL32 = MachineModel.LINUX32;

  @Parameters(name = "{2}: {0}")
  public static Object[][] machineModels() {
    Object[][] types =
        new Object[][] {
          // fieldname          // expected offset in bits
          {STRUCT, FIRST_BITFIELD_12, 0, MODEL32},
          {STRUCT, FIRST_BITFIELD_12, 0, MODEL64},
          {STRUCT, SECOND_BITFIELD_10, 12, MODEL32},
          {STRUCT, SECOND_BITFIELD_10, 12, MODEL64},
          {STRUCT, THIRD_INT, 32, MODEL32},
          {STRUCT, THIRD_INT, 32, MODEL64},
          {STRUCT, LAST_INCOMPLETEARRAY, 64, MODEL32},
          {STRUCT, LAST_INCOMPLETEARRAY, 64, MODEL64},
          {STRUCT_2, CHAR, 88, MODEL32},
          {STRUCT_2, CHAR, 88, MODEL64},
          {STRUCT_3, CHAR, 80, MODEL32},
          {STRUCT_3, CHAR, 80, MODEL64}
        };

    return types;
  }

  // If you plan on expanding this memberlist for more
  // tests of the struct, remember to update the expectation
  // for LAST_INCOMPLETEARRAY and please refrain from either
  // of these:
  //    a) (DO NOT) insert a new Member after LAST_INCOMPLETE
  //    b) (DO NOT) insert a new Member before THIRD_INT
  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(new CBitFieldType(CNumericTypes.UNSIGNED_INT, 12),
              FIRST_BITFIELD_12),
          new CCompositeTypeMemberDeclaration(new CBitFieldType(CNumericTypes.UNSIGNED_INT, 10),
              SECOND_BITFIELD_10),
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT),
          new CCompositeTypeMemberDeclaration(
              new CArrayType(false, false, CNumericTypes.LONG_LONG_INT, null),
              LAST_INCOMPLETEARRAY));

  // struct s { unsigned int a : 12; unsigned int b : 10; int c; long long d[]; };
  private static final CCompositeType STRUCT =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_2 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 15), LONG_BITFIELD_15),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 18), LONG_BITFIELD_18),
          new CCompositeTypeMemberDeclaration(CNumericTypes.CHAR, CHAR));

  // struct s { int a; long b : 15; long c : 18; char d; };
  private static final CCompositeType STRUCT_2 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS_2, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_3 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 17), LONG_BITFIELD_15),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 16), LONG_BITFIELD_18),
          new CCompositeTypeMemberDeclaration(CNumericTypes.CHAR, CHAR));

  // struct s { int a; long b : 18; long c : 15; char d; };
  private static final CCompositeType STRUCT_3 =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS_3, TEST_STRUCT, TEST_STRUCT);

  @Parameter(0)
  public CCompositeType testStruct;

  @Parameter(1)
  public String testField;

  @Parameter(2)
  public int expectedOffset;

  @Parameter(3)
  public MachineModel model;

  @Test
  public void testGetFieldOffsetInStruct() {
    assertThat(model.getFieldOffsetInBits(testStruct, testField)).isEqualTo(expectedOffset);
  }
}
