// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PrecisionType {
  PREDICATE;

  @Override
  @JsonValue
  public String toString() {
    return switch (this) {
      case PREDICATE -> "predicate";
    };
  }

  @JsonCreator
  public static PrecisionType fromKeyword(String keyword) {
    return switch (keyword) {
      case "predicate" -> PREDICATE;
      default -> throw new IllegalArgumentException("Unknown keyword: " + keyword);
    };
  }
}
