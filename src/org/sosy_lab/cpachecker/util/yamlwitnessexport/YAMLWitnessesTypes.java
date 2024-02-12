// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class YAMLWitnessesTypes {

  public enum WitnessVersion {
    V2,
    V3;

    @Override
    public String toString() {
      return switch (this) {
        case V2 -> "2.0";
        case V3 -> "3.0";
      };
    }
  }

  public enum ExpressionType {
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
    public static ExpressionType fromKeyword(String keyword) {
      return switch (keyword) {
        case "c_expression" -> C;
        case "ACSL" -> ACSL;
        default -> throw new IllegalArgumentException("Unknown keyword: " + keyword);
      };
    }
  }
}
