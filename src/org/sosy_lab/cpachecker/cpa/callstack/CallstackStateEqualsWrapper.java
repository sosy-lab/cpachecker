// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.base.Preconditions;
import java.util.Objects;

/**
 * This is a wrapper for a {@link CallstackState}, which allows to check equality based on the
 * actual content of the stack.
 *
 * <p>This class is necessary, because (or as long as) we do not have a direct implementation of
 * {@link CallstackState#equals(Object)}.
 */
public class CallstackStateEqualsWrapper {

  private final CallstackState state;

  public CallstackStateEqualsWrapper(CallstackState pState) {
    state = Preconditions.checkNotNull(pState);
  }

  public CallstackState getState() {
    return state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CallstackStateEqualsWrapper)) {
      return false;
    }
    CallstackStateEqualsWrapper other = (CallstackStateEqualsWrapper) o;
    return stackLocationsAreEqual(state, other.getState());
  }

  private static boolean stackLocationsAreEqual(CallstackState a, CallstackState b) {
    if (a.getDepth() != b.getDepth()) {
      return false;
    }

    // check the whole stack
    while (a != null) {
      if (a == b) {
        return true;
      }
      if (!a.getCallNode().equals(b.getCallNode())
          || !a.getCurrentFunction().equals(b.getCurrentFunction())) {
        return false;
      }
      a = a.getPreviousState();
      b = b.getPreviousState();
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(state.getCallNode(), state.getCurrentFunction(), state.getDepth());
  }

  @Override
  public String toString() {
    return state.toString();
  }
}
