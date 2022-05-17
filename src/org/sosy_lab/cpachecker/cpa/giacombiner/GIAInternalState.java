// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giacombiner;

import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;

public class GIAInternalState implements AbstractGIAState{

private final AutomatonState internalState;


  public GIAInternalState(AutomatonState pInternalState) {
    internalState = pInternalState;
  }

  @Override
  public AbstractGIAState copy() {
    return new GIAInternalState(internalState);
  }

  @Override
  public boolean statePresent() {
    return true;
  }

  public AutomatonState getInternalState() {
    return internalState;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof GIAInternalState)) {
      return false;
    }
    GIAInternalState that = (GIAInternalState) pO;
    return Objects.equals(internalState, that.internalState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(internalState);
  }

  @Override
  public String toString() {
    return internalState.getInternalStateName();
  }
}
