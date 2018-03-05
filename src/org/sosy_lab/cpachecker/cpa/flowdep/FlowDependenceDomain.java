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
package org.sosy_lab.cpachecker.cpa.flowdep;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerDomain;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Abstract domain of {@link FlowDependenceCPA}. Top is the set of all possible flow dependences of
 * a program, bottom is the empty set.
 *
 * The domain is based on a power-set lattice.
 */
class FlowDependenceDomain implements AbstractDomain {

  private final PointerDomain pointerDomain = PointerDomain.INSTANCE;

  @Override
  public AbstractState join(
      AbstractState pState1, AbstractState pState2) throws CPAException, InterruptedException {

    assert pState1 instanceof FlowDependenceState
        : "Wrong type for first state passed: " + pState1;
    assert pState2 instanceof FlowDependenceState
        : "Wrong type for second state passed: " + pState2;

    if (isLessOrEqual(pState2, pState1)) {
      return pState2;
    } else {
      Pair<ReachingDefState, PointerState> wrapped1 = ((FlowDependenceState) pState1).unwrap();
      Pair<ReachingDefState, PointerState> wrapped2 = ((FlowDependenceState) pState2).unwrap();
      ReachingDefState joinedReachDefs = wrapped1.getFirst().join(wrapped2.getFirst());
      AbstractState joinedPointers = pointerDomain.join(wrapped1.getSecond(), wrapped2.getSecond());

      CompositeState joinedComposite =
          new CompositeState(ImmutableList.of(joinedReachDefs, joinedPointers));
      FlowDependenceState joinedFlowDeps = new FlowDependenceState(joinedComposite);

      joinedFlowDeps.addAll(((FlowDependenceState) pState1).getAll());

      for (Cell<CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, ProgramDefinitionPoint>>
          e : ((FlowDependenceState) pState2).getAll().cellSet()) {

        CFAEdge g = e.getRowKey();
        Optional<MemoryLocation> m = e.getColumnKey();

        joinedFlowDeps.addDependence(g, m, checkNotNull(e.getValue()));
      }
      return joinedFlowDeps;
    }
  }

  @Override
  public boolean isLessOrEqual(final AbstractState pStateLhs, final AbstractState pStateRhs)
      throws CPAException, InterruptedException {

    assert pStateLhs instanceof FlowDependenceState
        : "Wrong type for first state passed: " + pStateLhs;
    assert pStateRhs instanceof FlowDependenceState
        : "Wrong type for second state passed: " + pStateRhs;

    FlowDependenceState state1 = (FlowDependenceState) pStateLhs;
    FlowDependenceState state2 = (FlowDependenceState) pStateRhs;

    Pair<ReachingDefState, PointerState> wrapped1 = state1.unwrap();
    Pair<ReachingDefState, PointerState> wrapped2 = state2.unwrap();
    boolean reachedLessOrEqual = wrapped1.getFirst().isLessOrEqual(wrapped2.getFirst());
    if (reachedLessOrEqual) {
      boolean pointerLessOrEqual =
          pointerDomain.isLessOrEqual(wrapped1.getSecond(), wrapped2.getSecond());
      return pointerLessOrEqual && containsAll(state2.getAll(), state1.getAll());
    } else {
      return false;
    }
  }

  private <R, C, V> boolean containsAll(Table<R, C, V> superTable, Table<R, C, V> subTable) {
    Set<Cell<R, C, V>> superEntries = superTable.cellSet();
    for (Cell<R, C, V> e : subTable.cellSet()) {
      if (!superEntries.contains(e)) {
        return false;
      }
    }
    return true;
  }
}
