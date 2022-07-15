// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
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
    justification = "Fields are filled by parameterization of JUnit")
public class MachineModelGetOffsetTest {

  private static final String TEST_STRUCT = "testStruct";

  private static final String FIRST_BITFIELD_12 = "firstBitfield";
  private static final String SECOND_BITFIELD_10 = "secondBitfield";
  private static final String THIRD_INT = "thirdInt";
  private static final String LAST_INCOMPLETEARRAY = "lastIncomplete";
  private static final String LONG_BITFIELD_15 = "long_bitfield_15";
  private static final String LONG_BITFIELD_18 = "long_bitfield_18";
  private static final String CHAR = "char";

  private static final MachineModel MODEL64 = MachineModel.LINUX64;
  private static final MachineModel MODEL32 = MachineModel.LINUX32;

  @Parameters(name = "{2}: {0}")
  public static Object[][] machineModels() {
    return new Object[][] {
      // struct, fieldname, expected offset in bits, machine model
      {STRUCT, FIRST_BITFIELD_12, BigInteger.ZERO, MODEL32},
      {STRUCT, FIRST_BITFIELD_12, BigInteger.ZERO, MODEL64},
      {STRUCT, SECOND_BITFIELD_10, BigInteger.valueOf(12), MODEL32},
      {STRUCT, SECOND_BITFIELD_10, BigInteger.valueOf(12), MODEL64},
      {STRUCT, THIRD_INT, BigInteger.valueOf(32), MODEL32},
      {STRUCT, THIRD_INT, BigInteger.valueOf(32), MODEL64},
      {STRUCT, LAST_INCOMPLETEARRAY, BigInteger.valueOf(64), MODEL32},
      {STRUCT, LAST_INCOMPLETEARRAY, BigInteger.valueOf(64), MODEL64},
      {STRUCT_2, CHAR, BigInteger.valueOf(88), MODEL32},
      {STRUCT_2, CHAR, BigInteger.valueOf(88), MODEL64},
      {STRUCT_3, CHAR, BigInteger.valueOf(80), MODEL32},
      {STRUCT_3, CHAR, BigInteger.valueOf(80), MODEL64}
    };
  }

  // If you plan on expanding this memberlist for more
  // tests of the struct, remember to update the expectation
  // for LAST_INCOMPLETEARRAY and please refrain from either
  // of these:
  //    a) (DO NOT) insert a new Member after LAST_INCOMPLETE
  //    b) (DO NOT) insert a new Member before THIRD_INT
  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.UNSIGNED_INT, 12), FIRST_BITFIELD_12),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.UNSIGNED_INT, 10), SECOND_BITFIELD_10),
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
  public BigInteger expectedOffset;

  @Parameter(3)
  public MachineModel model;

  @Test
  public void testGetFieldOffsetInStruct() {
    assertThat(model.getFieldOffsetInBits(testStruct, testField)).isEqualTo(expectedOffset);
  }
}
