// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giacombiner;

import java.util.Objects;

/** Represents bullet */
public class SplitGIAState implements AbstractGIAState {
  @Override
  public AbstractGIAState copy() {
    return new SplitGIAState();
  }

  @Override
  public boolean statePresent() {
    return false;
  }

  @Override
  public String toString() {
    return "Split";
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && (obj instanceof SplitGIAState);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(SplitGIAState.class);
  }
}
