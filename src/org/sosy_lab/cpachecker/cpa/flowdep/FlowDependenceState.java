/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
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

  private final Map<CFAEdge, Multimap<MemoryLocation, ProgramDefinitionPoint>> usedDefs;

  private ReachingDefState reachDefState;

  FlowDependenceState(ReachingDefState pReachDefState) {
    usedDefs = new HashMap<>();

    reachDefState = pReachDefState;
  }

  public Set<CFAEdge> getEdges() {
    return usedDefs.keySet();
  }

  public Multimap<MemoryLocation, ProgramDefinitionPoint> getDependentDefs(final CFAEdge pEdge) {
    return ImmutableMultimap.copyOf(usedDefs.get(pEdge));
  }

  /**
   * Adds a flow dependence based on the given variable and its definition at the given program
   * definition point.
   */
  public void addDependence(CFAEdge pEdge, Multimap<MemoryLocation, ProgramDefinitionPoint> pUses) {
    usedDefs.put(pEdge, pUses);
  }

  ReachingDefState getReachDefState() {
    return reachDefState;
  }

  @Override
  public FlowDependenceState join(FlowDependenceState other) {
    if (isLessOrEqual(other)) {
      return other;
    } else {
      ReachingDefState joinedReachDefs = reachDefState.join(other.reachDefState);
      FlowDependenceState joinedFlowDeps = new FlowDependenceState(joinedReachDefs);
      joinedFlowDeps.usedDefs.putAll(usedDefs);
      for (Map.Entry<CFAEdge, Multimap<MemoryLocation, ProgramDefinitionPoint>> e :
          other.usedDefs.entrySet()) {

        CFAEdge g = e.getKey();

        if (joinedFlowDeps.usedDefs.containsKey(g)) {
          joinedFlowDeps.usedDefs.get(g).putAll(e.getValue());
        } else {
          joinedFlowDeps.usedDefs.put(g, e.getValue());
        }
      }
      return joinedFlowDeps;
    }
  }

  @Override
  public boolean isLessOrEqual(FlowDependenceState other) {
    return reachDefState.isLessOrEqual(other.reachDefState)
        && containsAll(other.usedDefs, usedDefs);
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
    return Objects.equals(reachDefState, that.reachDefState)
        && Objects.equals(usedDefs, that.usedDefs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reachDefState, usedDefs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    if (!usedDefs.isEmpty()) {
      sb.append("\n");
    }
    for (Map.Entry<CFAEdge, Multimap<MemoryLocation, ProgramDefinitionPoint>> e
        : usedDefs.entrySet()) {
      sb.append("\t");
      sb.append(e.getKey().toString()).append(":\n");
      for (Map.Entry<MemoryLocation, ProgramDefinitionPoint> memDefs : e.getValue().entries()) {
        sb.append("\t\t");
        sb.append(memDefs.getKey().toString()).append(" <- ").append(memDefs.getValue());
        sb.append("\n");
      }
    }
    sb.append("};\n");
    sb.append(reachDefState.toString());
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
    for (Map.Entry<CFAEdge, Multimap<MemoryLocation, ProgramDefinitionPoint>> e
        : usedDefs.entrySet()) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(e.getKey().toString()).append(": [ ");
      boolean first2 = true;
      for (Map.Entry<MemoryLocation, ProgramDefinitionPoint> memDefs : e.getValue().entries()) {
        if (!first2) {
          sb.append(", ");
        }
        first2 = false;
        sb.append(memDefs.getKey()).append(" <- ").append(memDefs.getValue());
      }
      sb.append("]");
    }
    sb.append("};");
    sb.append(reachDefState.toDOTLabel());
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
