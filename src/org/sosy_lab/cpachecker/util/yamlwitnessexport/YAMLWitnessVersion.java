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
  V3;

  @Override
  public String toString() {
    return switch (this) {
      case V2 -> "2.0";
      case V3 -> "3.0";
    };
  }
}
