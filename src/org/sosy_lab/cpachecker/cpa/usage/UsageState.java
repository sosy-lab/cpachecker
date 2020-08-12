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
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/** Represents one abstract state of the Usage CPA. */
public class UsageState extends AbstractSingleWrapperState
    implements LatticeAbstractState<UsageState>, AbstractStateWithEdge, AliasInfoProvider {
  /* Boilerplate code to avoid serializing this class */

  private static final long serialVersionUID = -898577877284268426L;
  private final transient StateStatistics stats;

  private transient ImmutableMap<AbstractIdentifier, AbstractIdentifier>
      variableBindingRelation;

  private UsageState(
      final AbstractState pWrappedElement,
      final ImmutableMap<AbstractIdentifier, AbstractIdentifier> pVarBind,
      final StateStatistics pStats) {
    super(pWrappedElement);
    variableBindingRelation = pVarBind;
    stats = pStats;
  }

  public static UsageState createInitialState(final AbstractState pWrappedElement) {
    return new UsageState(
        pWrappedElement,
        ImmutableMap.of(),
        new StateStatistics());
  }

  private UsageState(final AbstractState pWrappedElement, final UsageState state) {
    this(
        pWrappedElement,
        state.variableBindingRelation,
        state.stats);
  }

  public UsageState put(final AbstractIdentifier id1, final AbstractIdentifier id2) {
    if (!id1.equals(id2)) {
      UsageState result = new UsageState(this.getWrappedState(), this);
      // Optimization to store
      int d1 = id1.getDereference();
      AbstractIdentifier newId1 = id1.cloneWithDereference(0);
      int d2 = id2.getDereference();
      AbstractIdentifier newId2 = id2.cloneWithDereference(d2 - d1);
      ImmutableMap.Builder<AbstractIdentifier, AbstractIdentifier> builder = ImmutableMap.builder();

      for (Entry<AbstractIdentifier, AbstractIdentifier> entry : variableBindingRelation.entrySet()) {
        AbstractIdentifier key = entry.getKey();
        if (key.equals(newId1)) {
          // Can not remove from builder, so have to go through a map manually
          builder.put(newId1, newId2);
        } else {
          builder.put(entry);
        }
      }
      result.variableBindingRelation = builder.build();

      return result;
    }
    return this;
  }

  public AbstractIdentifier getLinksIfNecessary(final AbstractIdentifier id) {
    /* Special get!
     * If we get **b, having (*b, c), we give *c
     */
    AbstractIdentifier newId = id.cloneWithDereference(0);
    if (variableBindingRelation.containsKey(newId)) {
      int d = id.getDereference();
      AbstractIdentifier initialId = variableBindingRelation.get(newId);
      AbstractIdentifier pointsTo = initialId.cloneWithDereference(initialId.getDereference() + d);
      return getLinksIfNecessary(pointsTo);
    }

    return id;
  }

  public UsageState copy(final AbstractState pWrappedState) {
    return new UsageState(pWrappedState, this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(variableBindingRelation);
    result = prime * result + super.hashCode();
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
    boolean b =
        Objects.equals(variableBindingRelation, other.variableBindingRelation)
        && getWrappedState().equals(other.getWrappedState());

    return b;
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
  public Set<AbstractIdentifier> getAllPossibleAliases(AbstractIdentifier id) {
    AbstractIdentifier newId = getLinksIfNecessary(id);
    if (newId != id) {
      return ImmutableSet.of(newId);
    } else {
      return ImmutableSet.of();
    }
  }

  @Override
  public void filterAliases(AbstractIdentifier pIdentifier, Collection<AbstractIdentifier> pSet) {
    AbstractIdentifier newId = getLinksIfNecessary(pIdentifier);
    if (newId != pIdentifier) {
      pSet.remove(pIdentifier);
    }
  }

  @Override
  public UsageState join(UsageState pOther) {
    stats.joinTimer.start();

    ImmutableMap.Builder<AbstractIdentifier, AbstractIdentifier> newRelation =
        ImmutableMap.builder();
    newRelation.putAll(variableBindingRelation);

    for (Entry<AbstractIdentifier, AbstractIdentifier> entry :
        pOther.variableBindingRelation.entrySet()) {
      newRelation.put(entry.getKey(), entry.getValue());
    }
    stats.joinTimer.stop();
    return new UsageState(this.getWrappedState(), newRelation.build(), stats);
  }

  protected Object readResolve() {
    return new UsageState(
        getWrappedState(),
        ImmutableMap.of(),
        new StateStatistics());
  }

  @Override
  public AbstractEdge getAbstractEdge() {
    return ((AbstractStateWithEdge) getWrappedState()).getAbstractEdge();
  }

  @Override
  public boolean hasEmptyEffect() {
    return ((AbstractStateWithEdge) getWrappedState()).hasEmptyEffect();
  }

  @Override
  public boolean isProjection() {
    AbstractState wrapped = getWrappedState();
    if (wrapped instanceof AbstractStateWithEdge) {
      return ((AbstractStateWithEdge) wrapped).isProjection();
    } else {
      return false;
    }
  }
}
