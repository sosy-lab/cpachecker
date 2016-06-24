/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.callstack;

import java.util.Objects;

import com.google.common.base.Preconditions;

/**
 * This is a wrapper for a {@link CallstackState},
 * which allows to check equality based on the actual content of the stack.
 *
 * This class is necessary, because (or as long as) we do not have
 * a direct implementation of {@link CallstackState#equals(Object)}.
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
    if (o == this) { return true; }
    if (!(o instanceof CallstackStateEqualsWrapper)) { return false; }
    CallstackState other = ((CallstackStateEqualsWrapper) o).getState();
    CallstackState tmp = state;
    if (other.getDepth() != tmp.getDepth()) { return false; }

    // check the whole stack
    while (tmp != null) {
      if (other == tmp) { return true; }
      if (!other.getCallNode().equals(tmp.getCallNode())
          || !other.getCurrentFunction().equals(tmp.getCurrentFunction())) { return false; }
      other = other.getPreviousState();
      tmp = tmp.getPreviousState();
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