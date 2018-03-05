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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Abstract state of {@link FlowDependenceCPA}. Each state consists of a set of used memory
 * locations that are mapped to their possibly active definitions.
 *
 * <p>All objects of this class are immutable.
 */
public class FlowDependenceState implements AbstractState, AbstractWrapperState, Graphable {

  private final Table<
          CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, ProgramDefinitionPoint>>
      usedDefs;

  private CompositeState reachDefState;

  FlowDependenceState(CompositeState pReachDefState) {
    usedDefs = HashBasedTable.create();

    reachDefState = pReachDefState;
  }

  public Set<CFAEdge> getDependees() {
    return usedDefs.rowKeySet();
  }

  public Set<Optional<MemoryLocation>> getDefinitions(final CFAEdge pForEdge) {
    return usedDefs.row(pForEdge).keySet();
  }

  public Multimap<MemoryLocation, ProgramDefinitionPoint> getDependentDefs(
      final CFAEdge pEdge, final Optional<MemoryLocation> pDefinition) {
    return ImmutableMultimap.copyOf(usedDefs.get(pEdge, pDefinition));
  }

  /**
   * Adds a flow dependence based on the given variable and its definition at the given program
   * definition point.
   */
  public void addDependence(
      CFAEdge pEdge,
      Optional<MemoryLocation> pDefines,
      Multimap<MemoryLocation, ProgramDefinitionPoint> pUses) {

    if (usedDefs.contains(pEdge, pDefines)) {
      usedDefs.get(pEdge, pDefines).putAll(pUses);
    } else {
      usedDefs.put(pEdge, pDefines, pUses);
    }
  }

  CompositeState getReachDefState() {
    return reachDefState;
  }

  void addAll(
      Table<CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, ProgramDefinitionPoint>>
          pUsedDefs) {
    usedDefs.putAll(pUsedDefs);
  }

  Table<CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, ProgramDefinitionPoint>>
      getAll() {
    return usedDefs;
  }

  public Pair<ReachingDefState, PointerState> unwrap() {
    ImmutableList<AbstractState> wrappedStates = reachDefState.getWrappedStates();
    ReachingDefState reachdef = null;
    PointerState pointers = null;
    assert wrappedStates.size() == 2 : "Wrapped state has wrong size: " + wrappedStates.size();
    for (AbstractState s : wrappedStates) {
      if (s instanceof ReachingDefState) {
        reachdef = (ReachingDefState) s;
      } else if (s instanceof PointerState) {
        pointers = (PointerState) s;
      } else {
        throw new AssertionError("Wrong state type: " + s.getClass().getSimpleName());
      }
    }
    return Pair.of(reachdef, pointers);
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
    for (Cell<CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, ProgramDefinitionPoint>>
        e : usedDefs.cellSet()) {
      sb.append("\t");
      sb.append(checkNotNull(e.getRowKey()).toString());
      Optional<MemoryLocation> possibleDef = checkNotNull(e.getColumnKey());
      if (possibleDef.isPresent()) {
        sb.append(" + ").append(possibleDef.toString());
      }
      sb.append(":\n");
      for (Map.Entry<MemoryLocation, ProgramDefinitionPoint> memDefs : checkNotNull(e.getValue()).entries()) {
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
    for (Cell<CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, ProgramDefinitionPoint>>
        e : usedDefs.cellSet()) {
      if (!first) {
        sb.append(", ");
      }
      first = false;

      CFAEdge edge = checkNotNull(e.getRowKey());
      Optional<MemoryLocation> possibleDef = checkNotNull(e.getColumnKey());
      Multimap<MemoryLocation, ProgramDefinitionPoint> uses = checkNotNull(e.getValue());
      sb.append(edge.toString());
      if (possibleDef.isPresent()) {
        sb.append(" + ").append(possibleDef.toString());
      }
      sb.append(": [ ");
      boolean first2 = true;
      for (Map.Entry<MemoryLocation, ProgramDefinitionPoint> memDefs : uses.entries()) {
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
