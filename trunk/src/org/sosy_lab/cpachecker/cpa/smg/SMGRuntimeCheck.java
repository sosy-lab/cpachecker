// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

public enum SMGRuntimeCheck {
  FORCED(-1),
  NONE(0),
  HALF(1),
  FULL(2);

  private final int id;

  SMGRuntimeCheck(int pId) {
    id = pId;
  }

  public int getValue() {
    return id;
  }

  public boolean isFinerOrEqualThan(SMGRuntimeCheck other) {
    return id >= other.id;
  }
}
