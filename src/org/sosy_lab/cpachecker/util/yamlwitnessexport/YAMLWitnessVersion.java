// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

public enum YAMLWitnessVersion {
  V2,
  V2d1,
  V2dG;

  @Override
  public String toString() {
    return switch (this) {
      case V2 -> "2.0";
      case V2d1 -> "2.1";
      case V2dG -> "2.G";
    };
  }
}
