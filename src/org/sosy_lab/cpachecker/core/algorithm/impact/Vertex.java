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
package org.sosy_lab.cpachecker.core.algorithm.impact;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * This class represents the vertices/abstract states used by the
 * {@link ImpactAlgorithm}.
 * This class is basically similar to {@link AbstractState},
 * but allows only one parent and additionally stores a modifiable state formula.
 */
@SuppressFBWarnings("SE_BAD_FIELD")
class Vertex extends AbstractSingleWrapperState {
  /* Boilerplate code to avoid serializing this class */
  private static final long serialVersionUID = 0xDEADBEEF;

  /**
   * javadoc to remove unused parameter warning
   * @param out the output stream
   */
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    throw new NotSerializableException();
  }

  private static int nextId = 0;
  private final int id = nextId++;

  private final @Nullable Vertex parent;
  private final BooleanFormulaManager bfmgr;

  private final List<Vertex> children = new ArrayList<>(2);

  private BooleanFormula stateFormula;

  private @Nullable Vertex coveredBy = null;
  private List<Vertex> coveredNodes = new ArrayList<>(0);

  public Vertex(BooleanFormulaManager bfmgr, BooleanFormula pStateFormula, AbstractState pElement) {
    super(pElement);
    this.bfmgr = bfmgr;
    parent = null;
    assert bfmgr.isTrue(pStateFormula);
    stateFormula = pStateFormula;
  }

  public Vertex(BooleanFormulaManager bfmgr, Vertex pParent, BooleanFormula pStateFormula, @Nullable AbstractState pElement) {
    super(pElement);
    this.bfmgr = bfmgr;
    parent = checkNotNull(pParent);
    parent.children.add(this);
    stateFormula = checkNotNull(pStateFormula);
  }


  public BooleanFormula getStateFormula() {
    return stateFormula;
  }

  public CFAEdge getIncomingEdge() {
    CFANode thisLocation = AbstractStates.extractLocation(getWrappedState());
    CFANode parentLocation = AbstractStates.extractLocation(parent.getWrappedState());
    return parentLocation.getEdgeTo(thisLocation);
  }

  public void setCoveredBy(Vertex pCoveredBy) {
    assert !isCovered() : "Cannot re-cover the covered node " + this;
    assert !pCoveredBy.isCovered() : "Covered node " + pCoveredBy + " cannot cover";
    coveredBy = checkNotNull(pCoveredBy);
    pCoveredBy.coveredNodes.add(this);
  }

  /**
   * Uncover all nodes covered by this node.
   * @return a list of all nodes that were previously covered by this node
   */
  public List<Vertex> cleanCoverage() {
    assert !isCovered() || coveredNodes.isEmpty();
    if (coveredNodes.isEmpty()) {
      return Collections.emptyList();
    }

    List<Vertex> result = coveredNodes;
    coveredNodes = new ArrayList<>(0);

    for (Vertex v : result) {
      assert v.coveredBy == this;
      v.coveredBy = null; // uncover
    }

    return result;
  }

  public void setStateFormula(BooleanFormula pStateFormula) {
    stateFormula = checkNotNull(pStateFormula);
  }

  public Vertex getParent() {
    checkState(hasParent());
    return parent;
  }

  public List<Vertex> getChildren() {
    return Collections.unmodifiableList(children);
  }

  public List<Vertex> getSubtree() {
    List<Vertex> subtreeNodes = new ArrayList<>();
    subtreeNodes.add(this);
    getSubtree(subtreeNodes);
    return subtreeNodes;
  }

  private void getSubtree(List<Vertex> subtreeNodes) {
    subtreeNodes.addAll(children);
    for (Vertex v : children) {
      v.getSubtree(subtreeNodes);
    }
  }

  public boolean hasParent() {
    return parent != null;
  }

  public boolean isCovered() {
    if (coveredBy != null) {
      return true;
    }

    // now check recursively all parents
    Vertex v = this;
    while (v.hasParent()) {
      v = v.getParent();

      if (v.coveredBy != null) {
        return true;
      }
    }

    return false;
  }

  public boolean isLeaf() {
    return children.isEmpty() && AbstractStates.extractLocation(getWrappedState()).getNumLeavingEdges() > 0;
  }

  @Override
  public boolean isTarget() {
    return !bfmgr.isFalse(stateFormula) && super.isTarget();
  }

  public boolean isAncestorOf(Vertex v) {
    if (this == v) {
      return true;
    }

    while (v.hasParent()) {
      v = v.getParent();
      if (this == v) {
        return true;
      }
    }
    return false;
  }

  public boolean isOlderThan(Vertex v) {
    return (id < v.id);
  }

  @Override
  public String toString() {
    return "Id: " + id + " " + stateFormula.toString() + "\n" + super.toString();
  }
}
