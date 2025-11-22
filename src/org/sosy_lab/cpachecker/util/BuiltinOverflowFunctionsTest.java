// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

public class BuiltinOverflowFunctionsTest {

  // This is all GNU (GCC) defined builtin (integer) overflow functions according to
  // https://gcc.gnu.org/onlinedocs/gcc/Integer-Overflow-Builtins.html
  private static final Set<String> allGNUBuiltinOverflowFunctions =
      ImmutableSet.of(
          "__builtin_add_overflow",
          "__builtin_sadd_overflow",
          "__builtin_saddl_overflow",
          "__builtin_saddll_overflow",
          "__builtin_uadd_overflow",
          "__builtin_uaddl_overflow",
          "__builtin_uaddll_overflow",
          "__builtin_sub_overflow",
          "__builtin_ssub_overflow",
          "__builtin_ssubl_overflow",
          "__builtin_ssubll_overflow",
          "__builtin_usub_overflow",
          "__builtin_usubl_overflow",
          "__builtin_usubll_overflow",
          "__builtin_mul_overflow",
          "__builtin_smul_overflow",
          "__builtin_smull_overflow",
          "__builtin_smulll_overflow",
          "__builtin_umul_overflow",
          "__builtin_umull_overflow",
          "__builtin_umulll_overflow",
          "__builtin_add_overflow_p",
          "__builtin_sub_overflow_p",
          "__builtin_mul_overflow_p",
          "__builtin_addc",
          "__builtin_addcl",
          "__builtin_addcll",
          "__builtin_subc",
          "__builtin_subcl",
          "__builtin_subcll");

  @Test
  public void functionNameRecognizedTest() {
    for (String fun : allGNUBuiltinOverflowFunctions) {
      assertThat(BuiltinOverflowFunctions.isBuiltinOverflowFunction(fun)).isTrue();
    }
  }

  @Test
  public void allFunctionsAvailableTest() {
    // With functionNameRecognizedTest() this makes sure all are recognized
    assertThat(BuiltinOverflowFunctions.getAllFunctions())
        .hasSize(allGNUBuiltinOverflowFunctions.size());
  }

  @Test
  public void testParameterTypesOfBooleanResultReturningBuiltinOverflowFunctionsWithSideEffects() {
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_uadd_overflow"))
        .containsExactly(
            CNumericTypes.UNSIGNED_INT,
            CNumericTypes.UNSIGNED_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_sadd_overflow"))
        .containsExactly(
            CNumericTypes.INT,
            CNumericTypes.INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT));

    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_usub_overflow"))
        .containsExactly(
            CNumericTypes.UNSIGNED_INT,
            CNumericTypes.UNSIGNED_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_ssub_overflow"))
        .containsExactly(
            CNumericTypes.INT,
            CNumericTypes.INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT));

    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_umul_overflow"))
        .containsExactly(
            CNumericTypes.UNSIGNED_INT,
            CNumericTypes.UNSIGNED_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_smul_overflow"))
        .containsExactly(
            CNumericTypes.INT,
            CNumericTypes.INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.INT));

    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_uaddl_overflow"))
        .containsExactly(
            CNumericTypes.UNSIGNED_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_LONG_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_saddl_overflow"))
        .containsExactly(
            CNumericTypes.LONG_INT,
            CNumericTypes.LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.LONG_INT));

    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_usubl_overflow"))
        .containsExactly(
            CNumericTypes.UNSIGNED_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_LONG_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_ssubl_overflow"))
        .containsExactly(
            CNumericTypes.LONG_INT,
            CNumericTypes.LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.LONG_INT));

    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_umull_overflow"))
        .containsExactly(
            CNumericTypes.UNSIGNED_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_LONG_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_smull_overflow"))
        .containsExactly(
            CNumericTypes.LONG_INT,
            CNumericTypes.LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.LONG_INT));

    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_uaddll_overflow"))
        .containsExactly(
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_LONG_LONG_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_saddll_overflow"))
        .containsExactly(
            CNumericTypes.LONG_LONG_INT,
            CNumericTypes.LONG_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.LONG_LONG_INT));

    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_usubll_overflow"))
        .containsExactly(
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_LONG_LONG_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_ssubll_overflow"))
        .containsExactly(
            CNumericTypes.LONG_LONG_INT,
            CNumericTypes.LONG_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.LONG_LONG_INT));

    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_umulll_overflow"))
        .containsExactly(
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_LONG_LONG_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_smulll_overflow"))
        .containsExactly(
            CNumericTypes.LONG_LONG_INT,
            CNumericTypes.LONG_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.LONG_LONG_INT));
  }

  @Test
  public void testParameterTypesOfArithmeticResultReturningBuiltinOverflowFunctions() {
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_addc"))
        .containsExactly(
            CNumericTypes.UNSIGNED_INT,
            CNumericTypes.UNSIGNED_INT,
            CNumericTypes.UNSIGNED_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_addcl"))
        .containsExactly(
            CNumericTypes.UNSIGNED_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_LONG_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_addcll"))
        .containsExactly(
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_LONG_LONG_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_subc"))
        .containsExactly(
            CNumericTypes.UNSIGNED_INT,
            CNumericTypes.UNSIGNED_INT,
            CNumericTypes.UNSIGNED_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_subcl"))
        .containsExactly(
            CNumericTypes.UNSIGNED_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_LONG_INT));
    assertThat(BuiltinOverflowFunctions.getParameterTypes("__builtin_subcll"))
        .containsExactly(
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            CNumericTypes.UNSIGNED_LONG_LONG_INT,
            new CPointerType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_LONG_LONG_INT));
  }
}
