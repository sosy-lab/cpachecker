// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.model;

public final class TargetSetTop implements TargetSet {

  public static final TargetSet INSTANCE = new TargetSetTop();

  private TargetSetTop() {}

  @Override
  public AliasType alias(TargetSet pOther) {
    return pOther.equals(TargetSetBot.INSTANCE) ? AliasType.NOT : AliasType.MAY;
  }

  @Override
  public TargetSet union(TargetSet pOther) {
    return this;
  }

  @Override
  public boolean includes(TargetSet pOther) {
    return true;
  }

  @Override
  public String toString() {
    return "\u22A4";
  }
}
