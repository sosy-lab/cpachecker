/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ecp.translators;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.ECPGuard;

public class GuardedState {

  private NondeterministicFiniteAutomaton.State mState;
  private Set<ECPGuard> mGuards;

  public GuardedState(NondeterministicFiniteAutomaton.State pCurrentState) {
    mState = pCurrentState;
    mGuards = Collections.emptySet();
  }

  public GuardedState(NondeterministicFiniteAutomaton.State pState, Set<ECPGuard> pGuards) {
    mState = pState;
    mGuards = pGuards;
  }

  public GuardedState(NondeterministicFiniteAutomaton.State pState, GuardedState pPreceedingState, Set<ECPGuard> pGuards) {
    mState = pState;
    mGuards = new HashSet<ECPGuard>();
    mGuards.addAll(pPreceedingState.mGuards);
    mGuards.addAll(pGuards);
  }

  public NondeterministicFiniteAutomaton.State getState() {
    return mState;
  }

  public Set<ECPGuard> getGuards() {
    return mGuards;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass().equals(getClass())) {
      GuardedState lGuardedState = (GuardedState)pOther;

      return mState.equals(lGuardedState.mState) && mGuards.equals(lGuardedState.mGuards);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mState.hashCode() + mGuards.hashCode() + 870890;
  }

  @Override
  public String toString() {
    return "(" + mState.toString() + ", " + mGuards.toString() + ")";
  }

  public boolean covers(GuardedState pState) {
    if (!mState.equals(pState.mState)) {
      return false;
    }

    return pState.mGuards.containsAll(mGuards);
  }

}
