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
  ACSL;

  @Override
  @JsonValue
  public String toString() {
    return switch (this) {
      case C -> "c_expression";
      case ACSL -> "ACSL";
    };
  }

  @JsonCreator
  public static YAMLWitnessExpressionType fromKeyword(String keyword) {
    return switch (keyword) {
      case "c_expression" -> C;
      case "ACSL" -> ACSL;
      default -> throw new IllegalArgumentException("Unknown keyword: " + keyword);
    };
  }
}
