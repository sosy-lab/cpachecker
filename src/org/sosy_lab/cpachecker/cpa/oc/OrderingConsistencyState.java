// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * One node of one thread instance's exploration DAG. States at the same {@link MergeKey} are merged
 * (guards become disjunctions), so the per-thread structure follows the unrolled control flow
 * instead of its path tree; states are never covered.
 */
public final class OrderingConsistencyState
    implements AbstractState, AbstractStateWithLocation, Targetable, Partitionable, Graphable {

  /**
   * Everything that must coincide for two states to describe the same unrolled program point. The
   * callstack is compared semantically (function/call-site chain).
   */
  record MergeKey(
      int instanceId,
      CFANode location,
      ImmutableList<String> callstack,
      ImmutableMap<CFANode, Integer> loopCounts,
      ImmutableMap<String, Integer> createCounts,
      ImmutableMap<String, Integer> threadHandles,
      ImmutableMap<String, Integer> lockDepths,
      boolean target) {}

  private static final ImmutableSet<TargetInformation> TARGET_INFORMATION =
      SimpleTargetInformation.singleton("error function called");

  private final int instanceId;
  private final LocationState locationState;
  private final CallstackState callstackState;
  private final PathFormula pathFormula;
  private final BooleanFormula guard;
  private final ImmutableList<Integer> lastEventIds;
  private final ImmutableMap<String, Integer> createCounts;
  private final ImmutableMap<String, Integer> threadHandles;
  private final ImmutableMap<CFANode, Integer> loopCounts;
  private final ImmutableMap<String, Integer> lockDepths;
  private final boolean target;

  // set once the state's successors have been computed; expanded states must not be merged into,
  // otherwise their suffix would be explored twice with overlapping guards
  private boolean expanded = false;

  // set when this state was merged into another one; the stop operator then covers it so that
  // only the merged state continues (merge and stop must agree, else both copies explore)
  private boolean absorbed = false;

  OrderingConsistencyState(
      int pInstanceId,
      LocationState pLocationState,
      CallstackState pCallstackState,
      PathFormula pPathFormula,
      BooleanFormula pGuard,
      ImmutableList<Integer> pLastEventIds,
      ImmutableMap<String, Integer> pCreateCounts,
      ImmutableMap<String, Integer> pThreadHandles,
      ImmutableMap<CFANode, Integer> pLoopCounts,
      ImmutableMap<String, Integer> pLockDepths,
      boolean pTarget) {
    instanceId = pInstanceId;
    locationState = pLocationState;
    callstackState = pCallstackState;
    pathFormula = pPathFormula;
    guard = pGuard;
    lastEventIds = pLastEventIds;
    createCounts = pCreateCounts;
    threadHandles = pThreadHandles;
    loopCounts = pLoopCounts;
    lockDepths = pLockDepths;
    target = pTarget;
  }

  int getInstanceId() {
    return instanceId;
  }

  LocationState getLocationState() {
    return locationState;
  }

  CallstackState getCallstackState() {
    return callstackState;
  }

  PathFormula getPathFormula() {
    return pathFormula;
  }

  BooleanFormula getGuard() {
    return guard;
  }

  ImmutableList<Integer> getLastEventIds() {
    return lastEventIds;
  }

  ImmutableMap<String, Integer> getCreateCounts() {
    return createCounts;
  }

  ImmutableMap<String, Integer> getThreadHandles() {
    return threadHandles;
  }

  ImmutableMap<CFANode, Integer> getLoopCounts() {
    return loopCounts;
  }

  ImmutableMap<String, Integer> getLockDepths() {
    return lockDepths;
  }

  boolean isExpanded() {
    return expanded;
  }

  void markExpanded() {
    expanded = true;
  }

  boolean isAbsorbed() {
    return absorbed;
  }

  void markAbsorbed() {
    absorbed = true;
  }

  MergeKey getMergeKey() {
    List<String> frames = new ArrayList<>();
    for (CallstackState frame = callstackState; frame != null; frame = frame.getPreviousState()) {
      frames.add(
          frame.getCurrentFunction()
              + "@"
              + (frame.getCallNode() == null ? "root" : frame.getCallNode().getNodeNumber()));
    }
    return new MergeKey(
        instanceId,
        locationState.getLocationNode(),
        ImmutableList.copyOf(frames),
        loopCounts,
        createCounts,
        threadHandles,
        lockDepths,
        target);
  }

  @Override
  public Object getPartitionKey() {
    return getMergeKey();
  }

  @Override
  public CFANode getLocationNode() {
    return locationState.getLocationNode();
  }

  @Override
  public boolean isTarget() {
    return target;
  }

  @Override
  public ImmutableSet<TargetInformation> getTargetInformation() {
    return TARGET_INFORMATION;
  }

  @Override
  public String toDOTLabel() {
    return "T" + instanceId + "@" + locationState.getLocationNode() + (target ? " TARGET" : "");
  }

  @Override
  public boolean shouldBeHighlighted() {
    return target;
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }
}
