// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

/**
 * Enum for the possible storage classes of C declarations. REGISTER is missing because it is
 * semantically equal to AUTO.
 */
public enum CStorageClass {
  AUTO,
  STATIC,
  EXTERN,
  TYPEDEF,
  ;

  public String toASTString() {
    if (equals(AUTO)) {
      return "";
    }
    return name().toLowerCase() + " ";
  }
}
