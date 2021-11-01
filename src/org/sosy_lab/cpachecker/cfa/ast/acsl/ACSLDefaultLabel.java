// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

enum ACSLDefaultLabel implements ACSLLabel {
  HERE("Here"),
  OLD("Old"),
  PRE("Pre"),
  POST("Post"),
  LOOP_ENTRY("LoopEntry"),
  LOOP_CURRENT("LoopCurrent"),
  INIT("Init");

  private final String name;

  ACSLDefaultLabel(String pName) {
    name = pName;
  }

  @Override
  public String toString() {
    return name;
  }
}
