// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum YAMLWitnessExpressionType {
  C,
  EXT_C,
  ACSL;

  @Override
  @JsonValue
  public String toString() {
    return switch (this) {
      case C -> "c_expression";
      case EXT_C -> "ext_c_expression";
      case ACSL -> "acsl_expression";
    };
  }

  @JsonCreator
  public static YAMLWitnessExpressionType fromKeyword(String keyword) {
    return switch (keyword) {
      case "c_expression" -> C;
      case "ext_c_expression" -> EXT_C;
      case "acsl_expression" -> ACSL;
      default -> throw new IllegalArgumentException("Unknown keyword: " + keyword);
    };
  }
}
