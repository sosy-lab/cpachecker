// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.directexport;

public class DataTypes {

  public enum WitnessVersion {
    V2,
    V3;

    @Override
    public String toString() {
      switch (this) {
        case V2:
          return "2.0";
        case V3:
          return "3.0";
      }
      return "Unknown";
    }
  }

  public enum ExpressionType {
    C,
    ACSL;

    @Override
    public String toString() {
      switch (this) {
        case C:
          return "c_expression";
        case ACSL:
          return "ACSL";
      }
      return "Unknown";
    }
  }
}
