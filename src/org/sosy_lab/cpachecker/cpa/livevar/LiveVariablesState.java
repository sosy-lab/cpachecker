// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.livevar;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.LiveVariables;

import java.util.BitSet;
import java.util.Objects;


class LiveVariablesState implements LatticeAbstractState<LiveVariablesState>, Graphable {

  private final BitSet liveVars;
  private final LiveVariablesTransferRelation manager;

  private LiveVariablesState(
      BitSet pLiveVariables,
      LiveVariablesTransferRelation pManager) {
    manager = pManager;
    checkNotNull(pLiveVariables);
    liveVars = pLiveVariables;
  }

  static LiveVariablesState empty(
      int totalNoVars,
      LiveVariablesTransferRelation pManager) {
    return new LiveVariablesState(new BitSet(totalNoVars), pManager);
  }

  static LiveVariablesState of(
      BitSet pLiveVars,
      LiveVariablesTransferRelation pManager
  ) {
    return new LiveVariablesState((BitSet) pLiveVars.clone(), pManager);
  }

  /**
   * Create new LiveVariablesState, but do not defensively copy the data:
   * the caller has to ensure that no other copies of {@code pLiveVars} exist.
   *
   * <p>Use at your own risk.
   */
  static LiveVariablesState ofUnique(
      BitSet pLiveVars,
      LiveVariablesTransferRelation pManager
  ) {
    return new LiveVariablesState(pLiveVars, pManager);
  }

  LiveVariablesState(int totalNoVars, LiveVariablesTransferRelation pManager) {
    liveVars = new BitSet(totalNoVars);
    manager = pManager;
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
    return LiveVariablesState.ofUnique(copy, manager);
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
    BitSet copy = (BitSet)liveVars.clone();
    copy.or(pOther.liveVars);
    if (copy.equals(pOther.liveVars)) {
      return pOther;
    }
    return ofUnique(copy, manager);
  }

  @Override
  public boolean isLessOrEqual(LiveVariablesState pOther) {
    return isSubsetOf(pOther);
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(", ").appendTo(sb, toStringIterable());
    sb.append("]");
    return sb.toString();
  }

  @Override
  public String toString() {
    return Joiner.on(", ").join(toStringIterable());
  }

  private Iterable<String> toStringIterable() {
    return FluentIterable.from(manager.dataToVars(liveVars)).transform(
        LiveVariables.FROM_EQUIV_WRAPPER_TO_STRING
    );
  }

  BitSet getDataCopy() {
    return (BitSet) liveVars.clone();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

}
