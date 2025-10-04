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
  PREDICATES,
  MEMORY_LOCATIONS;

  @Override
  @JsonValue
  public String toString() {
    return switch (this) {
      case PREDICATES -> "predicates";
      case MEMORY_LOCATIONS -> "memory_locations";
    };
  }

  @JsonCreator
  public static PrecisionType fromKeyword(String keyword) {
    return switch (keyword) {
      case "predicates" -> PREDICATES;
      case "memory_locations" -> MEMORY_LOCATIONS;
      default -> throw new IllegalArgumentException("Unknown keyword: " + keyword);
    };
  }
}
