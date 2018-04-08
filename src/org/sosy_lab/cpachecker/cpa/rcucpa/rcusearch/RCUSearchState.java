/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.rcucpa.rcusearch;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class RCUSearchState implements AbstractWrapperState {
  private final Set<MemoryLocation> rcuPointers;
  private final PointerState pointerState;

  public RCUSearchState(Set<MemoryLocation> pointers, PointerState pPointerState) {
    rcuPointers = pointers;
    pointerState = pPointerState;
  }

  public RCUSearchState() {
    rcuPointers = new HashSet<>();
    pointerState = PointerState.INITIAL_STATE;
  }

  public Set<MemoryLocation> getRcuPointers() {
    return rcuPointers;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }

    RCUSearchState that = (RCUSearchState) pO;

    if (rcuPointers != null ? !rcuPointers.equals(that.rcuPointers) : that.rcuPointers != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return rcuPointers != null ? rcuPointers.hashCode() : 0;
  }

  @Override
  public String toString() {
    return rcuPointers.toString();
  }

  @Override
  public Iterable<AbstractState> getWrappedStates() {
    return Collections.singleton(pointerState);
  }

  public static RCUSearchState copyOf(RCUSearchState pState) {
    return new RCUSearchState(pState.rcuPointers, pState.pointerState);
  }
}
