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
package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;

public class CFANode implements Comparable<CFANode>, Serializable {

  private static final long serialVersionUID = 5168350921309486536L;

  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  private final int nodeNumber;

  // do not serialize edges, recursive traversal of the CFA causes a stack-overflow.
  // edge-list is final, except for serialization
  private transient List<CFAEdge> leavingEdges = new ArrayList<>(1);
  private transient List<CFAEdge> enteringEdges = new ArrayList<>(1);

  // is start node of a loop?
  private boolean isLoopStart = false;

  // in which function is that node?
  private final String functionName;

  // set of variables out of scope after this node.
  // lazy initialization: first null, then final set
  private Set<CSimpleDeclaration> outOfScopeVariables = null;

  // list of summary edges
  private FunctionSummaryEdge leavingSummaryEdge = null;
  private FunctionSummaryEdge enteringSummaryEdge = null;

  // reverse postorder sort id, smaller if it appears later in sorting
  private int reversePostorderId = 0;

  public CFANode(String pFunctionName) {
    assert !pFunctionName.isEmpty();

    functionName = pFunctionName;
    nodeNumber = idGenerator.getFreshId();
  }

  public int getNodeNumber() {
    return nodeNumber;
  }

  public int getReversePostorderId() {
    return reversePostorderId;
  }

  public void setReversePostorderId(int pId) {
    reversePostorderId = pId;
  }

  public void addLeavingEdge(CFAEdge pNewLeavingEdge) {
    checkArgument(pNewLeavingEdge.getPredecessor() == this,
        "Cannot add edge \"%s\" to node %s as leaving edge", pNewLeavingEdge, this);
    leavingEdges.add(pNewLeavingEdge);
  }

  public void removeLeavingEdge(CFAEdge pEdge) {
    boolean removed = leavingEdges.remove(pEdge);
    checkArgument(removed,
        "Cannot remove non-existing leaving edge \"%s\" from node %s", pEdge, this);
  }

  public int getNumLeavingEdges() {
    return leavingEdges.size();
  }

  public CFAEdge getLeavingEdge(int pIndex) {
    return leavingEdges.get(pIndex);
  }

  public void addEnteringEdge(CFAEdge pEnteringEdge) {
    checkArgument(pEnteringEdge.getSuccessor() == this,
        "Cannot add edge \"%s\" to node %s as entering edge", pEnteringEdge, this);
    enteringEdges.add(pEnteringEdge);
  }

  public void removeEnteringEdge(CFAEdge pEdge) {
    boolean removed = enteringEdges.remove(pEdge);
    checkArgument(removed,
        "Cannot remove non-existing entering edge \"%s\" from node %s", pEdge, this);
  }

  public int getNumEnteringEdges() {
    return enteringEdges.size();
  }

  public CFAEdge getEnteringEdge(int pIndex) {
    return enteringEdges.get(pIndex);
  }

  public CFAEdge getEdgeTo(CFANode pOther) {
    for (CFAEdge edge : leavingEdges) {
      if (edge.getSuccessor() == pOther) {
        return edge;
      }
    }

    throw new IllegalArgumentException("there is no edge from " + this + " to " + pOther);
  }

  public boolean hasEdgeTo(CFANode pOther) {
    boolean hasEdge = false;
    for (CFAEdge edge : leavingEdges) {
      if (edge.getSuccessor() == pOther) {
        hasEdge = true;
        break;
      }
    }

    return hasEdge;
  }

  public void setLoopStart() {
    isLoopStart = true;
  }

  public boolean isLoopStart() {
    return isLoopStart;
  }

  public String getFunctionName() {
    return functionName;
  }

  public void addEnteringSummaryEdge(FunctionSummaryEdge pEdge) {
    checkState(enteringSummaryEdge == null,
        "Cannot add two entering summary edges to node %s", this);
    enteringSummaryEdge = pEdge;
  }

  public void addLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    checkState(leavingSummaryEdge == null,
        "Cannot add two leaving summary edges to node %s", this);
    leavingSummaryEdge = pEdge;
  }

  public FunctionSummaryEdge getEnteringSummaryEdge() {
    return enteringSummaryEdge;
  }

  public FunctionSummaryEdge getLeavingSummaryEdge() {
    return leavingSummaryEdge;
  }

  public void removeEnteringSummaryEdge(FunctionSummaryEdge pEdge) {
    checkArgument(enteringSummaryEdge == pEdge,
        "Cannot remove non-existing entering summary edge \"%s\" from node \"%s\"", pEdge, this);
    enteringSummaryEdge = null;
  }

  public void removeLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    checkArgument(leavingSummaryEdge == pEdge,
        "Cannot remove non-existing leaving summary edge \"%s\" from node \"%s\"", pEdge, this);
    leavingSummaryEdge = null;
  }

  @Override
  public String toString() {
    return "N" + nodeNumber;
  }

  @Override
  public final int compareTo(CFANode pOther) {
    return Integer.compare(this.nodeNumber, pOther.nodeNumber);
  }

  @Override
  public final boolean equals(Object pObj) {
    // Object.equals() is consistent with our compareTo()
    // because nodeNumber is a unique identifier.
    return super.equals(pObj);
  }

  @Override
  public final int hashCode() {
    // Object.hashCode() is consistent with our compareTo()
    // because nodeNumber is a unique identifier.
    return super.hashCode();
  }

  /**
   * Return a human-readable string describing to which point in the program
   * this state belongs to.
   * Returns the empty string if no suitable description can be found.
   *
   * Normally CFANodes do not belong to a file location,
   * so this should be used only as a best-effort guess to give a user
   * at least something to hold on.
   * Whenever possible, use the file locations of edges instead.
   */
  public String describeFileLocation() {
    if (this instanceof FunctionEntryNode) {
      return "entry of function " + getFunctionName()
          + " in " + ((FunctionEntryNode)this).getFileLocation();
    }

    if (this instanceof FunctionExitNode) {
      // these nodes do not belong to a location
      return "exit of function " + getFunctionName()
          + " in " + ((FunctionExitNode)this).getEntryNode().getFileLocation();
    }

    if (getNumLeavingEdges() > 0) {
      CFAEdge edge = getLeavingEdge(0);

      if (!edge.getFileLocation().equals(FileLocation.DUMMY)) {
        return "before " + edge.getFileLocation();
      }
    }

    if (getNumEnteringEdges() > 0) {
      CFAEdge edge = getEnteringEdge(0);

      if (!edge.getFileLocation().equals(FileLocation.DUMMY)) {
        return "after " + edge.getFileLocation();
      }
    }

    return "";
  }

  @SuppressWarnings("UnusedVariable") // parameter is required by API
  private void readObject(java.io.ObjectInputStream s)
      throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();

    // leaving and entering edges have to be updated explicitly after reading a node
    leavingEdges = new ArrayList<>(1);
    enteringEdges = new ArrayList<>(1);
  }

  public void addOutOfScopeVariables(Collection<CSimpleDeclaration> pOutOfScopeVariables) {
    if (outOfScopeVariables == null) { // lazy
      outOfScopeVariables = new LinkedHashSet<>();
    }
    outOfScopeVariables.addAll(pOutOfScopeVariables);
  }

  /**
   * Get a set of variables that were in scope before this node. We do not require them to have been
   * used in all paths before this node, i.e., we can also go out of scope of an "unused variable".
   * Variables can also go out of scope several times, i.e., when paths are merging and some paths
   * already lost a variable. All returned variables are guaranteed to be out of scope after
   * traversing this node, i.e., an analysis can remove their values from the program stack.
   * Variables can come into scope again, e.g. when iterating through a loop or calling a function
   * twice.
   *
   * <p>We currently do not return function parameters for function exit nodes. Additionally we do
   * not report any analysis-specific variables for encoding the return value of a function. Those
   * variables can be retrieved separately via {@link FunctionExitNode#getEntryNode()} or directly
   * in the analysis.
   */
  public Set<CSimpleDeclaration> getOutOfScopeVariables() {
    if (outOfScopeVariables == null) { // lazy
      return ImmutableSet.of();
    }
    return Collections.unmodifiableSet(outOfScopeVariables);
  }
}
