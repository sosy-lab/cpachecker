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
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

@RunWith(Parameterized.class)
@SuppressFBWarnings(
    value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
    justification = "Fields are filled by parameterization of JUnit")
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
      new CCompositeType(
          CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, FIELDS, TEST_STRUCT, TEST_STRUCT);

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
      new CCompositeType(
          CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, FIELDS_2, TEST_STRUCT, TEST_STRUCT);

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
      new CCompositeType(
          CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, FIELDS_3, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_4 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_LONG_INT, 8), LONGLONG_BITFIELD));

  // struct s { long long a : 8; };
  private static final CCompositeType STRUCT_4 =
      new CCompositeType(
          CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, FIELDS_4, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_5 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(CNumericTypes.CHAR, CHAR),
          new CCompositeTypeMemberDeclaration(CNumericTypes.LONG_LONG_INT, LONGLONG),
          new CCompositeTypeMemberDeclaration(
              new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT), INT_POINTER),
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
      new CCompositeType(
          CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, FIELDS_5, TEST_STRUCT, TEST_STRUCT);

  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS_6 =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 15), LONG_BITFIELD_15),
          new CCompositeTypeMemberDeclaration(
              new CBitFieldType(CNumericTypes.LONG_INT, 0), LONG_BITFIELD_0));

  //  struct s { int a; long b : 15; long : 0; };
  private static final CCompositeType STRUCT_6 =
      new CCompositeType(
          CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, FIELDS_6, TEST_STRUCT, TEST_STRUCT);

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
      new CCompositeType(
          CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, FIELDS_7, TEST_STRUCT, TEST_STRUCT);

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
      new CCompositeType(
          CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, FIELDS_8, TEST_STRUCT, TEST_STRUCT);

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
      new CCompositeType(
          CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, FIELDS_9, TEST_STRUCT, TEST_STRUCT);

  @Parameters(name = "{3}: {1}")
  public static Object[][] machineModels() {
    return new Object[][] {
      // struct, machine model, expected size according to gcc/c11, name
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
      {STRUCT_9, MODEL32, 12, "Struct_9"},
    };
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
    assertThat(Optional.of(model.getSizeof(testStruct))).hasValue(BigInteger.valueOf(expectedSize));
  }

  @Test
  public void testAtomicAlignment() {
    // C11 § 6.2.5 (27) / C23 § 6.2.5 (32): an atomic type may be aligned more strictly than its
    // non-atomic version. On 32-bit Linux an 8-byte atomic scalar is aligned to 8 (not 4).
    assertThat(MODEL32.getAlignof(CNumericTypes.LONG_LONG_INT)).isEqualTo(4);
    assertThat(MODEL32.getAlignof(CNumericTypes.LONG_LONG_INT.withAtomic())).isEqualTo(8);
    assertThat(MODEL32.getAlignof(CNumericTypes.DOUBLE)).isEqualTo(4);
    assertThat(MODEL32.getAlignof(CNumericTypes.DOUBLE.withAtomic())).isEqualTo(8);

    // long double is 12 bytes on 32-bit Linux, not a power of two, so its alignment is unchanged,
    // and types that are already naturally aligned are not affected either.
    assertThat(MODEL32.getAlignof(CNumericTypes.LONG_DOUBLE.withAtomic()))
        .isEqualTo(MODEL32.getAlignof(CNumericTypes.LONG_DOUBLE));
    assertThat(MODEL32.getAlignof(CNumericTypes.INT.withAtomic()))
        .isEqualTo(MODEL32.getAlignof(CNumericTypes.INT));

    // On 64-bit Linux there is no difference between atomic and non-atomic alignment.
    assertThat(MODEL64.getAlignof(CNumericTypes.LONG_LONG_INT.withAtomic()))
        .isEqualTo(MODEL64.getAlignof(CNumericTypes.LONG_LONG_INT));
    assertThat(MODEL64.getAlignof(CNumericTypes.DOUBLE.withAtomic()))
        .isEqualTo(MODEL64.getAlignof(CNumericTypes.DOUBLE));

    // The size of an atomic type never differs from the size of its non-atomic version.
    assertThat(MODEL32.getSizeof(CNumericTypes.LONG_LONG_INT.withAtomic()))
        .isEqualTo(BigInteger.valueOf(MODEL32.getSizeofLongLongInt()));

    // The same rule applies to composite (struct/union) types: STRUCT_1 (two 4-byte bitfields
    // plus an int) has size 8 but natural alignment 4 in both models, so the atomic version is
    // aligned to its size.
    assertThat(MODEL32.getAlignof(STRUCT_1)).isEqualTo(4);
    assertThat(MODEL32.getAlignof(STRUCT_1.withAtomic())).isEqualTo(8);
    assertThat(MODEL64.getAlignof(STRUCT_1)).isEqualTo(4);
    assertThat(MODEL64.getAlignof(STRUCT_1.withAtomic())).isEqualTo(8);
  }
}
