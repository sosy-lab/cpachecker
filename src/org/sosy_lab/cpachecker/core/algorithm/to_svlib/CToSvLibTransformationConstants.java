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

  private CToSvLibTransformationConstants() {
    throw new AssertionError("Cannot instantiate TransformationConstants");
  }
}
