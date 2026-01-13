// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Verify.verify;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ONE;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ZERO;
import static org.sosy_lab.cpachecker.cfa.types.c.CFunctionType.NO_ARGS_VOID_FUNCTION;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.BOOL;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.CHAR;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.DOUBLE;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.INT;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.LONG_DOUBLE;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.LONG_INT;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.SIGNED_CHAR;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.SIGNED_INT;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.SIGNED_LONG_INT;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.UNSIGNED_LONG_LONG_INT;
import static org.sosy_lab.cpachecker.cfa.types.c.CPointerType.POINTER_TO_CHAR;
import static org.sosy_lab.cpachecker.cfa.types.c.CPointerType.POINTER_TO_CONST_CHAR;
import static org.sosy_lab.cpachecker.cfa.types.c.CPointerType.POINTER_TO_VOID;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers.CONST;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers.CONST_VOLATILE;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers.VOLATILE;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypesTest.CONSTANT_ARRAY;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypesTest.TWO;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypesTest.UNKNOWN_ARRAY;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypesTest.VARIABLE_ARRAY;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypesTest.VAR_CONST;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypesTest.VAR_N;
import static org.sosy_lab.cpachecker.cfa.types.c.CVoidType.VOID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;

public class CTypeCompatibilityTest {

  private static final CEnumType ENUM_TYPE_CHAR =
      new CEnumType(
          CTypeQualifiers.NONE, CNumericTypes.CHAR, ImmutableList.of(), "e_char", "e_char");

  private static final CEnumType ENUM_TYPE_INT =
      new CEnumType(CTypeQualifiers.NONE, SIGNED_INT, ImmutableList.of(), "e_int", "e_int");

  private static final CEnumType ENUM_TYPE_LONG_LONG_INT =
      new CEnumType(
          CTypeQualifiers.NONE,
          CNumericTypes.UNSIGNED_LONG_LONG_INT,
          ImmutableList.of(),
          "e_ulonglong",
          "e_ulonglong");

  // For testing that a struct with a single int, a union with a single int, and a single int are
  // not compatible.
  private static final CCompositeType STRUCT_INT =
      new CCompositeType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(new CCompositeTypeMemberDeclaration(INT, "i")),
          "s_int",
          "s_int");
  private static final CCompositeType UNION_INT =
      new CCompositeType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.UNION,
          ImmutableList.of(new CCompositeTypeMemberDeclaration(INT, "i")),
          "s_int", // on purpose the same as for STRUCT_INT
          "s_int");
  private static final CCompositeType STRUCT_INT_INT =
      new CCompositeType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(
              new CCompositeTypeMemberDeclaration(INT, "i"),
              new CCompositeTypeMemberDeclaration(INT, "i2")),
          "s_int_int",
          "s_int_int");

  static final ImmutableSet<CType> TEST_TYPES =
      ImmutableSet.<CType>builder()
          .add(
              // ENUM_TYPE_CHAR, TODO #348
              ENUM_TYPE_INT,
              // ENUM_TYPE_LONG_LONG_INT, TODO #348
              STRUCT_INT,
              STRUCT_INT_INT,
              UNION_INT,
              CFunctionType.NO_ARGS_VOID_FUNCTION,
              CNumericTypes.BOOL,
              CNumericTypes.CHAR,
              CNumericTypes.CONST_CHAR,
              CNumericTypes.SIGNED_CHAR,
              CNumericTypes.UNSIGNED_CHAR,
              CNumericTypes.INT,
              // CNumericTypes.SIGNED_INT, // canonical version of the above
              CNumericTypes.UNSIGNED_INT,
              CNumericTypes.SHORT_INT,
              CNumericTypes.UNSIGNED_SHORT_INT,
              CNumericTypes.LONG_INT,
              // CNumericTypes.SIGNED_LONG_INT, // canonical version of the above
              CNumericTypes.UNSIGNED_LONG_INT,
              CNumericTypes.LONG_LONG_INT,
              // CNumericTypes.SIGNED_LONG_LONG_INT, // canonical version of the above
              CNumericTypes.UNSIGNED_LONG_LONG_INT,
              CNumericTypes.FLOAT,
              CNumericTypes.DOUBLE,
              CNumericTypes.LONG_DOUBLE,
              CNumericTypes.SIZE_T,
              CPointerType.POINTER_TO_VOID,
              CPointerType.POINTER_TO_CHAR,
              CPointerType.POINTER_TO_CONST_CHAR,
              CVoidType.VOID,
              CVoidType.CONST_VOID,
              CVoidType.VOLATILE_VOID,
              CVoidType.CONST_VOLATILE_VOID)
          .addAll(from(CTypesTest.parameters()).transform(data -> (CType) data[0]))
          .addAll(from(CTypeToStringTest.types()).transform(data -> (CType) data[1]))
          .build();

  private static void assertAreCompatible(CType type1, CType type2) {
    assertWithMessage("Type '%s' should be compatible to type '%s'", type1, type2)
        .that(CTypes.areTypesCompatible(type1, type2))
        .isTrue();
    assertWithMessage(
            "Type compatibility was not commutative for types '%s' and '%s'", type2, type1)
        .that(CTypes.areTypesCompatible(type2, type1))
        .isTrue();
  }

  private static void assertAreAllCompatible(CType pType1, CType pType2, CType... pTypes) {
    assertCompatibilityOfPairs(true, pType1, pType2, pTypes);
  }

  private static void assertAreIncompatible(CType type1, CType type2) {
    assertWithMessage("Type '%s' should not be compatible to type '%s'", type1, type2)
        .that(CTypes.areTypesCompatible(type1, type2))
        .isFalse();
    assertWithMessage(
            "Type compatibility was not commutative for types '%s' and '%s'", type2, type1)
        .that(CTypes.areTypesCompatible(type2, type1))
        .isFalse();
  }

  private static void assertAreAllIncompatible(CType pType1, CType pType2, CType... pTypes) {
    assertCompatibilityOfPairs(false, pType1, pType2, pTypes);
  }

  private static void assertCompatibilityOfPairs(
      boolean expected, CType pType1, CType pType2, CType... pTypes) {
    List<CType> allTypes = ImmutableList.<CType>builder().add(pType1, pType2).add(pTypes).build();

    for (List<CType> typePair : Lists.cartesianProduct(allTypes, allTypes)) {
      verify(typePair.size() == 2);
      @SuppressWarnings("SequencedCollectionGetFirst")
      CType type1 = typePair.get(0);
      CType type2 = typePair.get(1);
      if (type1 != type2) {
        assertWithMessage(
                "Type '%s' should %s compatible to type '%s'",
                type1, expected ? "be" : "not be", type2)
            .that(CTypes.areTypesCompatible(type1, type2))
            .isEqualTo(expected);
      }
    }
  }

  @Test
  public void testReflexiveTypeCompatibility() {
    for (CType type : TEST_TYPES) {
      assertWithMessage("Type '%s' should be compatible with itself", type)
          .that(CTypes.areTypesCompatible(type, type))
          .isTrue();
    }
  }

  @Test
  public void testReflexiveCanonicalTypeCompatibility() {
    for (CType type : TEST_TYPES) {
      assertAreCompatible(type, type.getCanonicalType());
    }
  }

  @Test
  public void testDifferentQualifiersCompatibility() {
    // C11 § 6.7.3 (10)
    for (CType type : from(TEST_TYPES)) {
      if (type instanceof CFunctionType
          || (type instanceof CArrayType arrayType
              && (arrayType.getType().isConst() || arrayType.getType().isVolatile()))) {
        continue;
      }
      CTypeQualifiers qualifiers = type.getQualifiers();
      CType differentAtomic =
          type.withQualifiersSetTo(qualifiers.withAtomicSetTo(!qualifiers.containsAtomic()));
      CType differentConst =
          type.withQualifiersSetTo(qualifiers.withConstSetTo(!qualifiers.containsConst()));
      CType differentVolatile =
          type.withQualifiersSetTo(qualifiers.withVolatileSetTo(!qualifiers.containsVolatile()));
      CType differentAll =
          type.withQualifiersSetTo(
              CTypeQualifiers.create(
                  !qualifiers.containsAtomic(),
                  !qualifiers.containsConst(),
                  !qualifiers.containsVolatile()));

      assertAreAllIncompatible(
          type, differentAtomic, differentConst, differentVolatile, differentAll);
    }
  }

  @Test
  public void testArrayCompatibilityDifferentElementType() {
    for (CType type1 : TEST_TYPES) {
      for (CType type2 : TEST_TYPES) {
        CArrayType arrayType1 = new CArrayType(CTypeQualifiers.NONE, type1);
        CArrayType arrayType2 = new CArrayType(CTypeQualifiers.NONE, type2);
        assertThat(CTypes.areTypesCompatible(arrayType1, arrayType2))
            .isEqualTo(CTypes.areTypesCompatible(type1, type2));

        CArrayType arrayType1Sized = new CArrayType(CTypeQualifiers.NONE, type1, ONE);
        CArrayType arrayType2Sized = new CArrayType(CTypeQualifiers.NONE, type2, ONE);
        assertThat(CTypes.areTypesCompatible(arrayType1Sized, arrayType2Sized))
            .isEqualTo(CTypes.areTypesCompatible(type1, type2));
      }
    }
  }

  @Test
  public void testArrayCompatibilitySameElementType() {
    for (CType type : TEST_TYPES) {
      CArrayType arrayType = new CArrayType(CTypeQualifiers.NONE, type);
      CArrayType arrayType0 = new CArrayType(CTypeQualifiers.NONE, type, ZERO);
      CArrayType arrayType1 = new CArrayType(CTypeQualifiers.NONE, type, ONE);
      CArrayType arrayType2 = new CArrayType(CTypeQualifiers.NONE, type, TWO);

      assertAreCompatible(arrayType, arrayType0);
      assertAreCompatible(arrayType, arrayType1);
      assertAreCompatible(arrayType, arrayType2);

      assertAreAllIncompatible(arrayType0, arrayType1, arrayType2);

      CArrayType arrayTypeVar = new CArrayType(CTypeQualifiers.NONE, type, VAR_N);
      assertAreCompatible(arrayTypeVar, arrayType);
      assertAreCompatible(arrayTypeVar, arrayType0);
      assertAreCompatible(arrayTypeVar, arrayType1);
      assertAreCompatible(arrayTypeVar, arrayType2);

      CArrayType arrayTypeConstVar = new CArrayType(CTypeQualifiers.NONE, type, VAR_CONST);
      assertAreCompatible(arrayTypeConstVar, arrayType);
      assertAreCompatible(arrayTypeConstVar, arrayType0);
      assertAreCompatible(arrayTypeConstVar, arrayType1);
      assertAreCompatible(arrayTypeConstVar, arrayType2);
    }
  }

  @Test
  public void testCompositeTypeCompatibility() {
    assertAreAllIncompatible(INT, STRUCT_INT, UNION_INT, STRUCT_INT_INT);
  }

  @Test
  @Ignore // https://gitlab.com/sosy-lab/software/cpachecker/-/issues/348#note_2709463414
  public void testEnumCompatibility() {
    // C11 § 6.7.2.2 (4)
    assertAreAllCompatible(ENUM_TYPE_CHAR, ENUM_TYPE_CHAR.getCompatibleType(), CHAR);
    assertAreAllCompatible(ENUM_TYPE_INT, ENUM_TYPE_INT.getCompatibleType(), INT);
    assertAreAllCompatible(
        ENUM_TYPE_LONG_LONG_INT,
        ENUM_TYPE_LONG_LONG_INT.getCompatibleType(),
        UNSIGNED_LONG_LONG_INT);
  }

  @Test
  public void testElaboratedTypeCompatibility() {
    for (CComplexType type : from(TEST_TYPES).filter(CComplexType.class)) {
      CElaboratedType elaboratedType =
          new CElaboratedType(
              CTypeQualifiers.NONE, type.getKind(), type.getName(), type.getOrigName(), type);

      assertAreCompatible(type, elaboratedType);
    }
  }

  @Test
  public void testFunctionCompatibility() {
    // The following two types should differ as much as possible while still being compatible.
    // TODO add more tests, in particular negative tests
    // int (volatile int, long int [], void ())
    CFunctionType type1 =
        new CFunctionType(
            INT,
            ImmutableList.of(
                INT.withVolatile(), new CArrayType(CONST, LONG_INT), NO_ARGS_VOID_FUNCTION),
            false);
    // signed int (signed int, long signed int * volatile, void (* const )())
    CFunctionType type2 =
        new CFunctionType(
            SIGNED_INT,
            ImmutableList.of(
                SIGNED_INT,
                new CPointerType(VOLATILE, SIGNED_LONG_INT),
                new CPointerType(CONST, NO_ARGS_VOID_FUNCTION)),
            false);

    assertAreCompatible(type1, type2);
  }

  @Test
  public void testFunctionCompatibilityExample5a() {
    // First part of Example 5 of C11 § 6.7.6.3 (simplified and with variable "c" instead of "m")
    CFunctionType type1 =
        new CFunctionType(
            DOUBLE,
            ImmutableList.of(
                new CArrayType(
                    CTypeQualifiers.NONE,
                    new CArrayType(CTypeQualifiers.NONE, DOUBLE, VAR_CONST),
                    VAR_N)),
            false);
    CFunctionType type2 =
        new CFunctionType(
            DOUBLE,
            ImmutableList.of(
                new CArrayType(CTypeQualifiers.NONE, new CArrayType(CTypeQualifiers.NONE, DOUBLE))),
            false);
    // We skip the third type because we do not distinguish between [] and [*]
    CFunctionType type4 =
        new CFunctionType(
            DOUBLE,
            ImmutableList.of(
                new CArrayType(
                    CTypeQualifiers.NONE, new CArrayType(CTypeQualifiers.NONE, DOUBLE, VAR_CONST))),
            false);

    assertAreAllCompatible(type1, type2, type4);
  }

  @Test
  public void testFunctionCompatibilityExample5b() {
    // Second part of Example 5 of C11 § 6.7.6.3 (simplified and with "const" instead of "restrict",
    // "1" instead of "3" and "2" instead of "5")
    CFunctionType type1 =
        new CFunctionType(
            VOID,
            ImmutableList.of(
                new CPointerType(CONST, new CArrayType(CTypeQualifiers.NONE, DOUBLE, TWO))),
            false);
    CFunctionType type2 =
        new CFunctionType(
            VOID,
            ImmutableList.of(
                new CArrayType(CONST, new CArrayType(CTypeQualifiers.NONE, DOUBLE, TWO))),
            false);
    CFunctionType type3 =
        new CFunctionType(
            VOID,
            ImmutableList.of(
                new CArrayType(CONST, new CArrayType(CTypeQualifiers.NONE, DOUBLE, TWO), ONE)),
            false);
    // We skip the fourth type because we have no way to represent "static" array lengths.

    assertAreAllCompatible(type1, type2, type3);
  }

  @Test
  public void testPointerCompatibility() {
    for (CType type1 : TEST_TYPES) {
      for (CType type2 : TEST_TYPES) {
        CPointerType pointerType1 = new CPointerType(CTypeQualifiers.NONE, type1);
        CPointerType pointerType2 = new CPointerType(CTypeQualifiers.NONE, type2);

        assertThat(CTypes.areTypesCompatible(pointerType1, pointerType2))
            .isEqualTo(CTypes.areTypesCompatible(type1, type2));
      }
    }
  }

  @Test
  public void testTypedefCompatibility() {
    for (CType type : TEST_TYPES) {
      CTypedefType typedef = new CTypedefType(CTypeQualifiers.NONE, "t", type);
      assertAreCompatible(type, typedef);

      if (!(type instanceof CFunctionType)) {
        CTypeQualifiers qualifiers =
            type.getCanonicalType() instanceof CArrayType arrayType
                ? arrayType.getType().getQualifiers()
                : type.getQualifiers();
        CTypedefType differentAtomic =
            new CTypedefType(qualifiers.withAtomicSetTo(!qualifiers.containsAtomic()), "t", type);
        CTypedefType differentVolatile =
            new CTypedefType(
                qualifiers.withVolatileSetTo(!qualifiers.containsVolatile()), "t", type);
        CTypedefType differentConst =
            new CTypedefType(qualifiers.withConstSetTo(!qualifiers.containsConst()), "t", type);
        CTypedefType differentAll =
            new CTypedefType(
                CTypeQualifiers.create(
                    !qualifiers.containsAtomic(),
                    !qualifiers.containsConst(),
                    !qualifiers.containsVolatile()),
                "t",
                type);

        if (!qualifiers.containsAtomic()) {
          assertAreIncompatible(type, differentAtomic);
        }
        if (!qualifiers.containsConst()) {
          assertAreIncompatible(type, differentConst);
        }
        if (!qualifiers.containsVolatile()) {
          assertAreIncompatible(type, differentVolatile);
        }
        if (!qualifiers.equals(CTypeQualifiers.ATOMIC_CONST_VOLATILE)) {
          assertAreIncompatible(type, differentAll);
        }
      }
    }
  }

  @Test
  public void testDifferentTypeCompatibility() {
    for (CType type1 : TEST_TYPES) {
      for (CType type2 : TEST_TYPES) {
        if (type1 == type2
            || (type1.getCanonicalType() instanceof CEnumType enumType1
                && enumType1
                    .getCompatibleType()
                    .getCanonicalType()
                    .equals(type2.getCanonicalType()))
            || (type2.getCanonicalType() instanceof CEnumType enumType2
                && enumType2
                    .getCompatibleType()
                    .getCanonicalType()
                    .equals(type1.getCanonicalType()))
            || (type1 instanceof CArrayType && type2 instanceof CArrayType)) {
          continue;
        }

        assertAreIncompatible(type1, type2);
      }
    }
  }

  @Test
  public void testArithmeticLhsAssignment() {
    // C11 § 6.5.16.1 (1) first item
    // If these extreme cases work, simpler types are expected to behave the same.
    // TODO add checks for complex types
    assertThat(CHAR.canBeAssignedFrom(DOUBLE)).isTrue();
    assertThat(BOOL.canBeAssignedFrom(DOUBLE)).isTrue();
    assertThat(BOOL.canBeAssignedFrom(ENUM_TYPE_LONG_LONG_INT)).isTrue();
    assertThat(SIGNED_CHAR.canBeAssignedFrom(UNSIGNED_LONG_LONG_INT)).isTrue();
    assertThat(ENUM_TYPE_CHAR.canBeAssignedFrom(LONG_DOUBLE)).isTrue();

    // C11 § 6.5.16.1 (1) last item
    assertThat(BOOL.canBeAssignedFrom(POINTER_TO_VOID)).isTrue();
    assertThat(BOOL.canBeAssignedFrom(POINTER_TO_CONST_CHAR)).isTrue();
  }

  @Test
  public void testComplexLhsAssignment() {
    // C11 § 6.5.16.1 (1) second item
    List<CType> complexTypes =
        from(TEST_TYPES).filter(t -> t.getCanonicalType() instanceof CCompositeType).toList();

    for (CType type1 : complexTypes) {
      for (CType type2 : complexTypes) {

        boolean compatible = CTypes.areTypesCompatible(type1, type2);
        assertThat(type1.canBeAssignedFrom(type2)).isEqualTo(compatible);
        assertThat(type2.canBeAssignedFrom(type1)).isEqualTo(compatible);
      }
    }
  }

  @Test
  public void testPointerLhsAssignment() {
    // C11 § 6.3.2.3 (2) or C11 § 6.5.16.1 (1) third item
    assertThat(POINTER_TO_CONST_CHAR.canBeAssignedFrom(POINTER_TO_CHAR)).isTrue();
    assertThat(POINTER_TO_CHAR.canBeAssignedFrom(POINTER_TO_CONST_CHAR)).isFalse();

    CPointerType intPointer = new CPointerType(CONST_VOLATILE, INT);
    assertThat(intPointer.canBeAssignedFrom(CONSTANT_ARRAY)).isTrue();
    assertThat(intPointer.canBeAssignedFrom(VARIABLE_ARRAY)).isTrue();
    assertThat(intPointer.canBeAssignedFrom(UNKNOWN_ARRAY)).isTrue();

    assertThat(POINTER_TO_CHAR.canBeAssignedFrom(CONSTANT_ARRAY)).isFalse();
    assertThat(POINTER_TO_CHAR.canBeAssignedFrom(VARIABLE_ARRAY)).isFalse();
    assertThat(POINTER_TO_CHAR.canBeAssignedFrom(UNKNOWN_ARRAY)).isFalse();

    for (CType type : TEST_TYPES) {
      if (!type.getCanonicalType().withoutQualifiers().equals(CHAR)
          && !(type.getCanonicalType() instanceof CVoidType)) {
        CPointerType pointerType = new CPointerType(CTypeQualifiers.NONE, type);
        assertThat(POINTER_TO_CHAR.canBeAssignedFrom(pointerType)).isFalse();
        assertThat(pointerType.canBeAssignedFrom(POINTER_TO_CHAR)).isFalse();
      }
    }
  }

  @Test
  public void testVoidPointerAssignment() {
    // C11 § 6.3.2.3 (1) or C11 § 6.5.16.1 (1) fourth item
    assertThat(POINTER_TO_VOID.canBeAssignedFrom(POINTER_TO_CHAR)).isTrue();
    assertThat(POINTER_TO_CHAR.canBeAssignedFrom(POINTER_TO_VOID)).isTrue();

    assertThat(POINTER_TO_VOID.canBeAssignedFrom(CONSTANT_ARRAY)).isTrue();
    assertThat(POINTER_TO_VOID.canBeAssignedFrom(VARIABLE_ARRAY)).isTrue();
    assertThat(POINTER_TO_VOID.canBeAssignedFrom(UNKNOWN_ARRAY)).isTrue();

    for (CType type : TEST_TYPES) {
      CPointerType pointerType = new CPointerType(CTypeQualifiers.NONE, type);
      assertThat(POINTER_TO_VOID.canBeAssignedFrom(pointerType)).isTrue();
      assertThat(pointerType.canBeAssignedFrom(POINTER_TO_VOID)).isTrue();
    }
  }

  @Test
  public void testArrayLhsAssignment() {
    // arrays can not be assigned to
    CPointerType intPointer = new CPointerType(CTypeQualifiers.NONE, INT);
    assertThat(CONSTANT_ARRAY.canBeAssignedFrom(POINTER_TO_VOID)).isFalse();
    assertThat(VARIABLE_ARRAY.canBeAssignedFrom(POINTER_TO_VOID)).isFalse();
    assertThat(UNKNOWN_ARRAY.canBeAssignedFrom(POINTER_TO_VOID)).isFalse();
    assertThat(CONSTANT_ARRAY.canBeAssignedFrom(intPointer)).isFalse();
    assertThat(VARIABLE_ARRAY.canBeAssignedFrom(intPointer)).isFalse();
    assertThat(UNKNOWN_ARRAY.canBeAssignedFrom(intPointer)).isFalse();
    assertThat(CONSTANT_ARRAY.canBeAssignedFrom(INT)).isFalse();
    assertThat(VARIABLE_ARRAY.canBeAssignedFrom(INT)).isFalse();
    assertThat(UNKNOWN_ARRAY.canBeAssignedFrom(INT)).isFalse();
  }

  @Test
  public void testIncompatibleAssignment() {
    // by exclusion (cases not listed in C11 § 6.5.16.1 (1)
    assertThat(INT.canBeAssignedFrom(POINTER_TO_VOID)).isFalse();
    assertThat(DOUBLE.canBeAssignedFrom(POINTER_TO_CONST_CHAR)).isFalse();

    assertThat(POINTER_TO_CHAR.canBeAssignedFrom(CHAR)).isFalse();
    assertThat(POINTER_TO_CHAR.canBeAssignedFrom(INT)).isFalse();

    assertThat(STRUCT_INT.canBeAssignedFrom(INT)).isFalse();
    assertThat(INT.canBeAssignedFrom(STRUCT_INT)).isFalse();
    assertThat(UNION_INT.canBeAssignedFrom(INT)).isFalse();
    assertThat(INT.canBeAssignedFrom(UNION_INT)).isFalse();
  }

  @Test
  @Ignore // cf. #1035
  public void testBrokenAssignmentCheck() {
    assertThat(POINTER_TO_VOID.canBeAssignedFrom(INT)).isFalse();
    assertThat(POINTER_TO_VOID.canBeAssignedFrom(ENUM_TYPE_CHAR)).isFalse();
  }
}
