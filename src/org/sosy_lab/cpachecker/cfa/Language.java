// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

/** Enumeration for Supported Languages. */
public enum Language {
  C,
  JAVA,
  LLVM;

  @Override
  public String toString() {
    switch (this) {
      case C:
        return "C";
      case JAVA:
        return "Java";
      case LLVM:
        return "LLVM IR";
      default:
        throw new AssertionError();
    }
  }
}
