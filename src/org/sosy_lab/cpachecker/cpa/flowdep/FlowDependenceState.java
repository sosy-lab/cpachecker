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

import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Abstract state of {@link FlowDependenceCPA}. Each state consists of a set of used memory
 * locations that are mapped to their possibly active definitions.
 *
 * <p>All objects of this class are immutable.
 */
public final class FlowDependenceState implements AbstractState, AbstractWrapperState, Graphable {

  /**
   * Data-flow dependencies. Represented by a table with entries of the form (g, m, D). {@link
   * FlowDependence} D is a set of tuples (d, g').
   *
   * <p>Read: A CFA edge g defines a memory location m, and since it uses d to do so, it is
   * data-flow dependent on g'.
   */
  private final Table<CFAEdge, Optional<MemoryLocation>, FlowDependence> usedDefs;

  private CompositeState reachDefState;

  FlowDependenceState(CompositeState pReachDefState) {
    usedDefs = HashBasedTable.create();

    reachDefState = pReachDefState;
  }

  public Set<CFAEdge> getAllEdgesWithDependencies() {
    return usedDefs.rowKeySet();
  }

  public Set<Optional<MemoryLocation>> getNewDefinitionsByEdge(final CFAEdge pForEdge) {
    return usedDefs.row(pForEdge).keySet();
  }

  public FlowDependence getDependenciesOfDefinitionAtEdge(
      final CFAEdge pEdge, final Optional<MemoryLocation> pDefinition) {
    return usedDefs.get(pEdge, pDefinition);
  }

  /**
   * Adds a flow dependence based on the given variable and its definition at the given program
   * definition point.
   */
  public void addDependence(
      CFAEdge pEdge, Optional<MemoryLocation> pDefines, FlowDependence pUses) {

    checkNotNull(pEdge);
    checkNotNull(pDefines);
    checkNotNull(pUses);

    if (usedDefs.contains(pEdge, pDefines)) {
      usedDefs.put(pEdge, pDefines, usedDefs.get(pEdge, pDefines).union(pUses));
    } else {
      usedDefs.put(pEdge, pDefines, pUses);
    }
  }

  CompositeState getReachDefState() {
    return reachDefState;
  }

  void addAll(Table<CFAEdge, Optional<MemoryLocation>, FlowDependence> pUsedDefs) {
    usedDefs.putAll(pUsedDefs);
  }

  Table<CFAEdge, Optional<MemoryLocation>, FlowDependence> getAll() {
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
    for (Cell<CFAEdge, Optional<MemoryLocation>, FlowDependence> e : usedDefs.cellSet()) {
      sb.append("\t");
      sb.append(checkNotNull(e.getRowKey()).toString());
      Optional<MemoryLocation> possibleDef = checkNotNull(e.getColumnKey());
      if (possibleDef.isPresent()) {
        sb.append(" + ").append(possibleDef.toString());
      }
      sb.append(":\n");
      sb.append(e.getValue());
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
    for (Cell<CFAEdge, Optional<MemoryLocation>, FlowDependence> e : usedDefs.cellSet()) {
      if (!first) {
        sb.append(", ");
      }
      first = false;

      CFAEdge edge = checkNotNull(e.getRowKey());
      Optional<MemoryLocation> possibleDef = checkNotNull(e.getColumnKey());
      Multimap<MemoryLocation, CFAEdge> uses = checkNotNull(e.getValue());
      sb.append(edge.toString());
      if (possibleDef.isPresent()) {
        sb.append(" + ").append(possibleDef.toString());
      }
      sb.append(": [ ");
      boolean first2 = true;
      for (Map.Entry<MemoryLocation, CFAEdge> memDefs : uses.entries()) {
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

  public static class FlowDependence extends ForwardingMultimap<MemoryLocation, CFAEdge> {

    private Multimap<MemoryLocation, CFAEdge> useToDefinitions;

    private FlowDependence() {
      useToDefinitions = HashMultimap.create();
    }

    public static FlowDependence copyOf() {
      return new FlowDependence();
    }

    public static FlowDependence copyOf(Multimap<MemoryLocation, CFAEdge> pMap) {
      return new FlowDependence(pMap);
    }

    public static FlowDependence create(Multimap<MemoryLocation, ProgramDefinitionPoint> pMap) {
      Multimap<MemoryLocation, CFAEdge> useToDefinitions = HashMultimap.create();
      for (Entry<MemoryLocation, ProgramDefinitionPoint> e : pMap.entries()) {
        boolean added = false;
        ProgramDefinitionPoint defPoint = e.getValue();
        CFANode start = defPoint.getDefinitionEntryLocation();
        CFANode stop = defPoint.getDefinitionExitLocation();

        for (CFAEdge g : CFAUtils.leavingEdges(start)) {
          if (g.getSuccessor().equals(stop)) {
            useToDefinitions.put(e.getKey(), g);
            added = true;
          }
        }
        assert added : "No edge added for nodes " + start + " to " + stop;
      }
      return copyOf(useToDefinitions);
    }

    private FlowDependence(Multimap<MemoryLocation, CFAEdge> pMap) {
      useToDefinitions = pMap;
    }

    @Override
    protected Multimap<MemoryLocation, CFAEdge> delegate() {
      return useToDefinitions;
    }

    public boolean isUnknownPointerDependence() {
      return false;
    }

    public FlowDependence union(final FlowDependence pOther) {
      if (isUnknownPointerDependence() || pOther.isUnknownPointerDependence()) {
        return UnknownPointerDependence.getInstance();
      } else {
        Multimap<MemoryLocation, CFAEdge> union =
            MultimapBuilder.hashKeys().hashSetValues().build(useToDefinitions);
        union.putAll(pOther.useToDefinitions);
        return new FlowDependence(union);
      }
    }

    @Override
    public boolean putAll(MemoryLocation key, Iterable<? extends CFAEdge> values) {
      throw new UnsupportedOperationException(
          "Unsupported. Use method #union() to ensure correct"
              + " handling of UnknownPointerDependence");
    }

    @Override
    public boolean putAll(Multimap<? extends MemoryLocation, ? extends CFAEdge> multimap) {
      throw new UnsupportedOperationException(
          "Unsupported. Use method #union() to ensure correct"
              + " handling of UnknownPointerDependence");
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (Entry<MemoryLocation, CFAEdge> memDefs : useToDefinitions.entries()) {
        sb.append("\t\t");
        sb.append(memDefs.getKey().toString()).append(" <- ").append(memDefs.getValue());
        sb.append("\n");
      }
      return sb.toString();
    }
  }

  public static class UnknownPointerDependence extends FlowDependence {

    private static final UnknownPointerDependence INSTANCE = new UnknownPointerDependence();

    private UnknownPointerDependence() {
      // It shouldn't be possible to add states to an UnknownPointerDependence
      super(ImmutableMultimap.of());
    }

    public static UnknownPointerDependence getInstance() {
      return INSTANCE;
    }

    @Override
    public boolean isUnknownPointerDependence() {
      return true;
    }

    @Override
    public boolean equals(Object object) {
      return object instanceof UnknownPointerDependence;
    }

    @Override
    public int hashCode() {
      return 1;
    }

    @Override
    public String toString() {
      return "Dependence to unknown memory location";
    }
  }
}
