// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

/** Helpers for the states that {@link ExecutionCPA} wraps. */
final class ExecutionStates {

  private ExecutionStates() {}

  /**
   * Create a state that is like the given one but uses the given state of the value analysis. This
   * is how {@link ExecutionCPA} changes values that the value analysis cannot compute on its own,
   * i.e. the values of a caller after a recursive call and the sampled values of the inputs.
   *
   * @return the new state, or {@code null} if the given state does not have a value analysis as a
   *     direct component
   */
  static @Nullable AbstractState withValueState(
      AbstractState pState, ValueAnalysisState pValueState) {
    if (!(pState instanceof CompositeState compositeState)) {
      return null;
    }
    List<AbstractState> components = new ArrayList<>(compositeState.getWrappedStates());
    for (int i = 0; i < components.size(); i++) {
      if (components.get(i) instanceof ValueAnalysisState) {
        components.set(i, pValueState);
        return new CompositeState(components);
      }
    }
    return null;
  }
}
