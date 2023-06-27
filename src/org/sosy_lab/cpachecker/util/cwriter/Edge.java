// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import java.util.Deque;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

class Edge implements Comparable<Edge> {

  private final ARGState parentState;
  private final ARGState childState;
  private final CFAEdge edge;
  private final Deque<FunctionBody> stack;

  public Edge(
      ARGState pChildElement, ARGState pParentElement, CFAEdge pEdge, Deque<FunctionBody> pStack) {
    childState = pChildElement;
    parentState = pParentElement;
    edge = pEdge;
    stack = pStack;
  }

  public ARGState getChildState() {
    return childState;
  }

  public ARGState getParentState() {
    return parentState;
  }

  public CFAEdge getEdge() {
    return edge;
  }

  public Deque<FunctionBody> getStack() {
    return stack;
  }

  /** comparison based on the child element */
  @Override
  public int compareTo(Edge pO) {
    return Integer.compare(getChildState().getStateId(), pO.getChildState().getStateId());
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof Edge) {
      return getChildState().getStateId() == ((Edge) pObj).getChildState().getStateId();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getChildState().getStateId();
  }
}
