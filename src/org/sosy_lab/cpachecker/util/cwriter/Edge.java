/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.cwriter;

import java.util.Stack;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

class Edge implements Comparable<Edge> {

  private final ARGState childState;
  private final CFAEdge edge;
  private final Stack<FunctionBody> stack;

  public Edge(ARGState pChildElement,
      CFAEdge pEdge, Stack<FunctionBody> pStack) {
    childState = pChildElement;
    edge = pEdge;
    stack = pStack;
  }

  public ARGState getChildState() {
    return childState;
  }

  public CFAEdge getEdge() {
    return edge;
  }

  public Stack<FunctionBody> getStack() {
    return stack;
  }

  @Override
  /** comparison based on the child element*/
  public int compareTo(Edge pO) {
    int otherElementId = pO.getChildState().getStateId();
    int thisElementId = this.getChildState().getStateId();

    if (thisElementId > otherElementId) {
      return 1;
    } else if (thisElementId < otherElementId) {
      return -1;
    }
    return 0;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (pObj instanceof Edge) {
      int otherElementId = ((Edge)pObj).getChildState().getStateId();
      int thisElementId = this.getChildState().getStateId();
      return thisElementId == otherElementId;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getChildState().getStateId();
  }
}
