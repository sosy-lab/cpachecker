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

public class GIAInternalState implements AbstractGIAState {

  private final AutomatonState automatonState;

  public GIAInternalState(AutomatonState pInternalState) {
    automatonState = pInternalState;
  }

  @Override
  public AbstractGIAState copy() {
    return new GIAInternalState(automatonState);
  }

  @Override
  public boolean statePresent() {
    return true;
  }

  public AutomatonState getAutomatonState() {
    return automatonState;
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
    return Objects.equals(
        automatonState.getInternalState(), that.automatonState.getInternalState());
  }

  @Override
  public int hashCode() {
    return Objects.hash(automatonState.getInternalState());
  }

  @Override
  public String toString() {
    return automatonState.getInternalStateName();
  }
}
