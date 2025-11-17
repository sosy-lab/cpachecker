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

public class BuiltinOverflowFunctionsTest {

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
}
