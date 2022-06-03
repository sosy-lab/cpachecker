// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class AutomatonTracker {

  private static AutomatonTracker instance = null;

  public static AutomatonTracker getInstance() {
    if (instance == null) {
      instance = new AutomatonTracker();
    }
    return instance;
  }

  private final Set<TracingInformation> coveredTransitions;

  private AutomatonTracker() {
    coveredTransitions = new LinkedHashSet<>();
  }

  public void track(TracingInformation pTracingInformation) {
    coveredTransitions.add(pTracingInformation);
  }

  public Set<TracingInformation> getCoveredTransitions() {
    return coveredTransitions;
  }

  public static class TracingInformation {

    private static final TracingInformation empty = new TracingInformation(null, null, null, null);

    private final CFAEdge edge;
    private final GraphMLTransition transition;
    private final AutomatonState from;
    private final AutomatonState to;

    private TracingInformation(
        AutomatonState pFrom,
        AutomatonState pTo,
        CFAEdge pEdge,
        GraphMLTransition pGraphMLTransition) {
      edge = pEdge;
      transition = pGraphMLTransition;
      from = pFrom;
      to = pTo;
    }

    public static TracingInformation of(
        AutomatonState pFrom, AutomatonState pTo, CFAEdge pEdge, GraphMLTransition pTransition) {
      if (pEdge == null && pTransition == null && pFrom == null) {
        return empty;
      }
      return new TracingInformation(pFrom, pTo, pEdge, pTransition);
    }

    public static TracingInformation empty() {
      return empty;
    }

    public boolean isEmpty() {
      return edge == null && transition == null && from == null && to == null;
    }

    public AutomatonState getFrom() {
      return from;
    }

    public AutomatonState getTo() {
      return to;
    }

    public CFAEdge getEdge() {
      return edge;
    }

    public GraphMLTransition getTransition() {
      return transition;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      TracingInformation that = (TracingInformation) pO;
      return com.google.common.base.Objects.equal(edge, that.edge)
          && com.google.common.base.Objects.equal(transition, that.transition)
          && com.google.common.base.Objects.equal(to, that.to)
          && com.google.common.base.Objects.equal(from, that.from);
    }

    @Override
    public int hashCode() {
      return com.google.common.base.Objects.hashCode(edge, transition, from, to);
    }
  }

}
