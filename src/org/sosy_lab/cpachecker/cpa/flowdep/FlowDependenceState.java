/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.flowdep;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Abstract state of {@link FlowDependenceCPA}. Each state consists of a set of used memory
 * locations that are mapped to their possibly active definitions.
 *
 * <p>
 * All objects of this class are immutable.
 * </p>
 */
public class FlowDependenceState
    implements AbstractState, AbstractWrapperState, LatticeAbstractState<FlowDependenceState>,
    Graphable {

  private final Multimap<MemoryLocation, ProgramDefinitionPoint> usedDefs;

  private ReachingDefState reachDefState;

  FlowDependenceState(ReachingDefState pReachDefState) {
    usedDefs = HashMultimap.create();

    reachDefState = pReachDefState;
  }

  public Collection<ProgramDefinitionPoint> getDependentDefs(final MemoryLocation pIdentifier) {
    return ImmutableSet.copyOf(usedDefs.get(pIdentifier));
  }

  public ImmutableMultimap<MemoryLocation, ProgramDefinitionPoint> getAllDependentDefs() {
    return ImmutableMultimap.copyOf(usedDefs);
  }

  /**
   * Adds a flow dependence based on the given variable and its definition at the given program
   * definition point.
   */
  public void addDependence(MemoryLocation pVarUsed, ProgramDefinitionPoint pVarDefinition) {
    usedDefs.put(pVarUsed, pVarDefinition);
  }

  /**
   * Adds one flow dependence based on the given variable for its definition at each given program
   * definition point.
   */
  public void
      addDependences(MemoryLocation pVarUsed, Iterable<ProgramDefinitionPoint> pVarDefinitions) {
    usedDefs.putAll(pVarUsed, pVarDefinitions);
  }

  ReachingDefState getReachDefState() {
    return reachDefState;
  }

  @Override
  public FlowDependenceState join(FlowDependenceState other) {
    if (other.reachDefState.equals(reachDefState) && other.usedDefs.equals(usedDefs)) {
      return other;
    } else {
      ReachingDefState joinedReachDefs = reachDefState.join(other.reachDefState);
      FlowDependenceState joinedFlowDeps = new FlowDependenceState(joinedReachDefs);
      joinedFlowDeps.usedDefs.putAll(usedDefs);
      joinedFlowDeps.usedDefs.putAll(other.usedDefs);
      return joinedFlowDeps;
    }
  }

  @Override
  public boolean isLessOrEqual(FlowDependenceState other)
      throws CPAException, InterruptedException {
    return reachDefState.isLessOrEqual(other.reachDefState)
        && containsAll(other.usedDefs.asMap(), usedDefs.asMap());
  }

  private <K, V> boolean containsAll(Map<K, V> superMap, Map<K, V> subMap) {
    Set<Map.Entry<K, V>> superEntries = superMap.entrySet();
    for (Map.Entry<K, V> e : subMap.entrySet()) {
      if (!superEntries.contains(e)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    FlowDependenceState that = (FlowDependenceState) pO;
    return Objects.equals(usedDefs, that.usedDefs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(usedDefs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    if (!usedDefs.isEmpty()) {
      sb.append("\n");
    }
    for (Map.Entry<MemoryLocation, ProgramDefinitionPoint> e : usedDefs.entries()) {
      sb.append("\t");
      sb.append(e.getKey().toString()).append(" <- ").append(e.getValue());
      sb.append("\n");
    }
    sb.append("}");
    return sb.toString();
  }

  @Override
  public Iterable<AbstractState> getWrappedStates() {
    return ImmutableSet.of(reachDefState);
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<MemoryLocation, ProgramDefinitionPoint> e : usedDefs.entries()) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(e.getKey().toString()).append(" <- ").append(e.getValue());
    }

    sb.append("}");
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
