// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableSet;

final class CToSvLibTransformationConstants {
  static final String INPUT_VAR_DUMMY_PREFIX = "__originalInput_";
  static final String RETURN_VAR_DUMMY_PREFIX = "__transformationDummyReturn_";
  static final String TMP_VAR_ASSIGNMENT = "__Transformation_TMP_VariableAssignment_";
  static final ImmutableSet<String> NAMES_OF_ASSERT_FUNCTIONS =
      ImmutableSet.of("__assert_fail", "__assert_perror_fail", "__assert");
  static final ImmutableSet<String> NAMES_OF_UNSUPPORTED_STDLIB_EXTERNAL_FUNCTIONS =
      ImmutableSet.of(
          "atof",
          "strtof",
          "strtold",
          "strtod",
          "strtol",
          "strtoul",
          "strtoq",
          "strtouq",
          "strtoll",
          "strtoull",
          "initstate",
          "setstate",
          "drand48",
          "erand48",
          "drand48_r",
          "erand48_r",
          "nrand48_r",
          "mrand48_r",
          "jrand48_r",
          "srand48_r",
          "seed48_r",
          "lcong48_r",
          "atexit",
          "at_quick_exit",
          "onexit",
          "getenv",
          "mktemp",
          "mkdtemp",
          "realpath",
          "bsearch",
          "ecvt",
          "fcvt",
          "gcvt",
          "qecvt",
          "qfcvt",
          "qgcvt",
          "ecvt_r",
          "fcvt_r",
          "qecvt_r",
          "qfcvt_r");
  static final ImmutableSet<String> NAMES_OF_UNSUPPORTED_NONDET_FUNCTIONS =
      ImmutableSet.of("__VERIFIER_nondet_float", "__VERIFIER_nondet_double");

  private CToSvLibTransformationConstants() {
    throw new AssertionError("Cannot instantiate TransformationConstants");
  }
}
