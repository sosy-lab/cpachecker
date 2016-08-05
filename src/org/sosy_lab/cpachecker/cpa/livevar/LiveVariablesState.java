/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.livevar;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.BitSet;
import java.util.Objects;


class LiveVariablesState implements LatticeAbstractState<LiveVariablesState>, Graphable {

  private final BitSet liveVars;

  LiveVariablesState(int totalNoVars) {
    liveVars = new BitSet(totalNoVars);
  }

  LiveVariablesState(BitSet pLiveVariables) {
    checkNotNull(pLiveVariables);
    liveVars = BitSet.valueOf(pLiveVariables.toLongArray());
  }

  LiveVariablesState union(LiveVariablesState pState2) {
    BitSet copy = (BitSet)liveVars.clone();
    copy.or(pState2.liveVars);
    if (copy.equals(pState2.liveVars)) {
      return pState2;
    }
    return new LiveVariablesState(copy);
  }

  boolean isSubsetOf(LiveVariablesState pState2) {
    BitSet copy = (BitSet)liveVars.clone();
    copy.or(pState2.liveVars);
    return copy.equals(pState2.liveVars);
  }

  boolean contains(int variableIdx) {
    return liveVars.get(variableIdx);
  }

  boolean containsAny(BitSet data) {
    BitSet copy = (BitSet)liveVars.clone();
    copy.and(data);
    return !copy.isEmpty();
  }

  LiveVariablesState removeLiveVariable(int posToRemove) {
    BitSet copy = (BitSet)liveVars.clone();
    copy.clear(posToRemove);
    return new LiveVariablesState(copy);
  }

  @Override
  public String toString() {
    // TODO
    return liveVars.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(liveVars);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof LiveVariablesState)) {
      return false;
    }

    LiveVariablesState other = (LiveVariablesState) obj;

    return Objects.equals(liveVars, other.liveVars);
  }

  @Override
  public LiveVariablesState join(LiveVariablesState pOther) {
    return union(pOther);
  }

  @Override
  public boolean isLessOrEqual(LiveVariablesState pOther) throws CPAException, InterruptedException {
    return isSubsetOf(pOther);
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    // todo
    sb.append(liveVars);
    sb.append("]");

    return sb.toString();
  }

  BitSet getData() {
    return BitSet.valueOf(liveVars.toLongArray());
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

}
