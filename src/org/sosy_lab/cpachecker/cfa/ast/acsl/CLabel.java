// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

class CLabel implements ACSLLabel {

  private String name;

  CLabel(String pName) {
    name = pName;
  }

  @Override
  public String toString() {
    return name;
  }
}
