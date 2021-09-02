// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.model;

public final class TargetSetBot implements TargetSet {

  public static final TargetSet INSTANCE = new TargetSetBot();

  private TargetSetBot() {}

  @Override
  public AliasType alias(TargetSet pOther) {
    return AliasType.NOT;
  }

  @Override
  public TargetSet union(TargetSet pOther) {
    return pOther;
  }

  @Override
  public boolean includes(TargetSet pOther) {
    return false;
  }

  @Override
  public String toString() {
    return "\u22A5";
  }
}
