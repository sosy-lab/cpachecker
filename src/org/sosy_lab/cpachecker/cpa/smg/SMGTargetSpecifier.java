// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

/** Target Specifier for specifying target of pointer. */
public enum SMGTargetSpecifier {
  REGION("reg"),
  FIRST("fst"),
  LAST("lst"),
  ALL("all"),
  OPT("optional"),
  UNKNOWN("unknown");

  private final String name;

  SMGTargetSpecifier(String pName) {
    name = pName;
  }

  @Override
  public String toString() {
    return name;
  }
}
