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
package org.sosy_lab.cpachecker.cpa.usage;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.Identifiers;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/** Represents one abstract state of the Usage CPA. */
public class UsageState extends AbstractSingleWrapperState
    implements LatticeAbstractState<UsageState> {

  private static final long serialVersionUID = -898577877284268426L;
  private final transient StateStatistics stats;

  private boolean isExitState;

  private transient PersistentSortedMap<AbstractIdentifier, AbstractIdentifier>
      variableBindingRelation;

  private UsageState(
      final AbstractState pWrappedElement,
      final PersistentSortedMap<AbstractIdentifier, AbstractIdentifier> pVarBind,
      final StateStatistics pStats,
      boolean exit) {
    super(pWrappedElement);
    variableBindingRelation = pVarBind;
    stats = pStats;
    isExitState = exit;
  }

  public static UsageState createInitialState(final AbstractState pWrappedElement) {
    return new UsageState(
        pWrappedElement, PathCopyingPersistentTreeMap.of(), new StateStatistics(), false);
  }

  private UsageState(final AbstractState pWrappedElement, final UsageState state) {
    this(
        pWrappedElement,
        state.variableBindingRelation,
        state.stats,
        state.isExitState);
  }

  public boolean containsLinks(final AbstractIdentifier id) {
    /* Special contains!
     *  if we have *b, map also contains **b, ***b and so on.
     *  So, if we get **b, having (*b, c), we give *c
     */
    return from(Identifiers.getDereferencedIdentifiers(id))
        .anyMatch(variableBindingRelation::containsKey);
  }

  public void put(final AbstractIdentifier id1, final AbstractIdentifier id2) {
    if (!id1.equals(id2)) {
      variableBindingRelation = variableBindingRelation.putAndCopy(id1, id2);
    }
  }

  public AbstractIdentifier getLinksIfNecessary(final AbstractIdentifier id) {

    if (!containsLinks(id)) {
      return id;
    }
    /* Special get!
     * If we get **b, having (*b, c), we give *c
     */
    Optional<AbstractIdentifier> linkedId =
        from(Identifiers.getDereferencedIdentifiers(id))
            .firstMatch(variableBindingRelation::containsKey);

    if (linkedId.isPresent()) {
      AbstractIdentifier pointsFrom = linkedId.get();
      int delta = id.getDereference() - pointsFrom.getDereference();
      AbstractIdentifier initialId = variableBindingRelation.get(pointsFrom);
      AbstractIdentifier pointsTo =
          initialId.cloneWithDereference(initialId.getDereference() + delta);
      if (this.containsLinks(pointsTo)) {
        pointsTo = getLinksIfNecessary(pointsTo);
      }
      return pointsTo;
    }

    return null;
  }

  public UsageState copy() {
    return copy(this.getWrappedState());
  }

  public UsageState copy(final AbstractState pWrappedState) {
    return new UsageState(pWrappedState, this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(variableBindingRelation);
    result = prime * super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    UsageState other = (UsageState) obj;
    return Objects.equals(variableBindingRelation, other.variableBindingRelation)
        && getWrappedState().equals(other.getWrappedState());
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();

    str.append("[");
    Joiner.on(", ").withKeyValueSeparator("->").appendTo(str, variableBindingRelation);
    str.append("]\n");
    str.append(getWrappedState());
    return str.toString();
  }

  @Override
  public boolean isLessOrEqual(final UsageState other) {
    // If we are here, the wrapped domain return true and the stop depends only on this value
    stats.lessTimer.start();
    // this element is not less or equal than the other element, if that one contains less elements
    if (this.variableBindingRelation.size() > other.variableBindingRelation.size()) {
      stats.lessTimer.stop();
      return false;
    }

    // also, this element is not less or equal than the other element,
    // if any one constant's value of the other element differs from the constant's value in this
    // element
    if (from(variableBindingRelation.keySet())
        .anyMatch(Predicates.not(other.variableBindingRelation::containsKey))) {
      stats.lessTimer.stop();
      return false;
    }

    stats.lessTimer.stop();
    return true;
  }

  public void asExitable() {
    isExitState = true;
  }

  public StateStatistics getStatistics() {
    return stats;
  }

  public static class StateStatistics {
    private StatTimer joinTimer = new StatTimer("Time for joining");
    private StatTimer lessTimer = new StatTimer("Time for cover check");

    public StateStatistics() {}

    public void printStatistics(StatisticsWriter out) {
      out.spacer().put(joinTimer).put(lessTimer);
    }
  }

  public static UsageState get(AbstractState state) {
    return AbstractStates.extractStateByType(state, UsageState.class);
  }

  @Override
  public boolean isExitState() {
    if (isExitState) {
      return true;
    }
    return super.isExitState();
  }

  @Override
  public UsageState join(UsageState pOther) {
    stats.joinTimer.start();
    PersistentSortedMap<AbstractIdentifier, AbstractIdentifier> newRelation =
        PathCopyingPersistentTreeMap.copyOf(this.variableBindingRelation);
    for (Entry<AbstractIdentifier, AbstractIdentifier> entry :
        pOther.variableBindingRelation.entrySet()) {
      newRelation = newRelation.putAndCopy(entry.getKey(), entry.getValue());
    }
    stats.joinTimer.stop();
    return new UsageState(this.getWrappedState(), newRelation, stats, isExitState);
  }

  protected Object readResolve() {
    return new UsageState(
        getWrappedState(),
        PathCopyingPersistentTreeMap.of(),
        new StateStatistics(),
        this.isExitState);
  }
}
