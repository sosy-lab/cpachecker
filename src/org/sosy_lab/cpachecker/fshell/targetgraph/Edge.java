/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.targetgraph;

import org.jgrapht.Graph;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

public class Edge {
  private Node mSource;
  private Node mTarget;
  private CFAEdge mCFAEdge;

  private int mHashCode;

  public Edge(Node pSource, Node pTarget, CFAEdge pCFAEdge, Graph<Node, Edge> pGraph) {
    assert(pSource != null);
    assert(pTarget != null);
    assert(pCFAEdge != null);
    assert(pGraph != null);

    mSource = pSource;
    mTarget = pTarget;
    mCFAEdge = pCFAEdge;

    mHashCode = 2341233 + mSource.hashCode() + mTarget.hashCode() + mCFAEdge.hashCode();

    // edge to graph
    pGraph.addVertex(mSource);
    pGraph.addVertex(mTarget);
    pGraph.addEdge(mSource, mTarget, this);
  }

  public Edge(Node pSource, Node pTarget, CFAEdge pCFAEdge) {
    assert(pSource != null);
    assert(pTarget != null);
    assert(pCFAEdge != null);

    mSource = pSource;
    mTarget = pTarget;
    mCFAEdge = pCFAEdge;

    mHashCode = 2341233 + mSource.hashCode() + mTarget.hashCode() + mCFAEdge.hashCode();
  }

  public Edge(Edge pEdge) {
    this(pEdge.getSource(), pEdge.getTarget(), pEdge.getCFAEdge());
  }

  @Override
  public int hashCode() {
    return mHashCode;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass() == getClass()) {
      Edge lEdge = (Edge)pOther;

      return lEdge.mSource.equals(mSource) && lEdge.mTarget.equals(mTarget) && lEdge.mCFAEdge.equals(mCFAEdge);
    }

    return false;
  }

  @Override
  public String toString() {
    return mSource.toString() + "-(" + mCFAEdge.toString() + ")>" + mTarget.toString();
  }

  public Node getSource() {
    return mSource;
  }

  public Node getTarget() {
    return mTarget;
  }

  public CFAEdge getCFAEdge() {
    return mCFAEdge;
  }

}
