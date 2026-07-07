// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * One node of one thread instance's exploration tree. States are never merged or covered (every
 * object is a distinct tree node), so object identity is the state identity and equals/hashCode are
 * intentionally not overridden.
 */
public final class OrderingConsistencyState implements AbstractState, Targetable, Graphable {

  private static final ImmutableSet<TargetInformation> TARGET_INFORMATION =
      SimpleTargetInformation.singleton("error function called");

  private final int instanceId;
  private final LocationState locationState;
  private final CallstackState callstackState;
  private final PathFormula pathFormula;
  private final BooleanFormula guard;
  private final int lastEventId;
  private final ImmutableMap<String, Integer> createCounts;
  private final ImmutableMap<String, Integer> threadHandles;
  private final ImmutableMap<CFANode, Integer> loopCounts;
  private final boolean target;

  OrderingConsistencyState(
      int pInstanceId,
      LocationState pLocationState,
      CallstackState pCallstackState,
      PathFormula pPathFormula,
      BooleanFormula pGuard,
      int pLastEventId,
      ImmutableMap<String, Integer> pCreateCounts,
      ImmutableMap<String, Integer> pThreadHandles,
      ImmutableMap<CFANode, Integer> pLoopCounts,
      boolean pTarget) {
    instanceId = pInstanceId;
    locationState = pLocationState;
    callstackState = pCallstackState;
    pathFormula = pPathFormula;
    guard = pGuard;
    lastEventId = pLastEventId;
    createCounts = pCreateCounts;
    threadHandles = pThreadHandles;
    loopCounts = pLoopCounts;
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

  int getLastEventId() {
    return lastEventId;
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
