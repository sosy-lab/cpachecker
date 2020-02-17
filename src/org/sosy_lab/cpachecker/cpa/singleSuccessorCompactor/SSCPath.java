/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SSCPath extends ARGPath {

  /** the reached-set determines the precision for states. */
  // TODO if we ever want to use SSCPath for more than "instant reporting",
  // we might need to extract precisions on SSCPath-creation.
  private final ARGReachedSet reachedSet;

  /** transfer needed to re-compute the missing states of SSC-chains. */
  private final SingleSuccessorCompactorTransferRelation transfer;

  public SSCPath(
      SingleSuccessorCompactorCPA pSscCpa,
      ARGReachedSet pMainReachedSet,
      ImmutableList<ARGState> pStates) {
    super(pStates);
    reachedSet = pMainReachedSet;
    transfer = pSscCpa.getTransferRelation();
  }

  @Override
  protected List<CFAEdge> buildFullPath() {
    List<CFAEdge> newFullPath = new ArrayList<>();
    PathIterator it = pathIterator();
    while (it.hasNext()) {
      getInnerEdgesFromTo(newFullPath, it.getAbstractState(), it.getNextAbstractState());
      it.advance();
    }

    assert checkFullPath(newFullPath) : Joiner.on("\n").join(Iterables.skip(newFullPath, 0));
    return newFullPath;
  }

  private boolean checkFullPath(List<CFAEdge> pFullPath) {
    if (pFullPath.isEmpty()) {
      return true; // trivial case
    }
    Iterator<CFAEdge> it = pFullPath.iterator();
    CFAEdge previous = it.next();
    while (it.hasNext()) {
      CFAEdge current = it.next();
      if (!Objects.equals(previous.getSuccessor(), current.getPredecessor())) {
        return false;
      }
      previous = current;
    }
    return true;
  }

  /**
   * unroll SSC-chains, get edges (at least one edge) from parent state to child state.
   *
   * @param newFullPath where the resulting edges are added to.
   */
  private void getInnerEdgesFromTo(List<CFAEdge> newFullPath, ARGState prev, ARGState succ) {
    final List<AbstractState> innerStates = new ArrayList<>();
    try {
      @SuppressWarnings("unused") // only inner states are important
      Collection<? extends AbstractState> successors =
          transfer.getAbstractSuccessorsWithList(
              prev.getWrappedState(), reachedSet.asReachedSet().getPrecision(prev), innerStates);
    } catch (CPATransferException | InterruptedException e) {
      throw new AssertionError("should not happen");
    }
    assert !innerStates.isEmpty();

    Iterator<AbstractState> innerIt = innerStates.iterator();
    AbstractState parent = innerIt.next();
    while (innerIt.hasNext()) {
      AbstractState child = innerIt.next();
      getEdgesFromTo(newFullPath, parent, child);
      parent = child;
    }

    // we ignore the new successor states and simply use the previously computed successor.
    getEdgesFromTo(newFullPath, parent, succ);
  }

  /**
   * unroll multi-edges, get edges (at least one edge) from parent state to child state.
   *
   * <p>Note: we can not really guarantee a full path here, but only a chain of edges starting at
   * the parent leading to some arbitrary location. Currently, this provides all edges from parent
   * to child in a well-defined way, i.e., it matches our intention of multi-edges.
   *
   * @param newFullPath where the resulting edges are added to.
   */
  private void getEdgesFromTo(
      List<CFAEdge> newFullPath, AbstractState parent, AbstractState child) {
    CFANode parentLoc = extractLocation(parent);
    CFANode childLoc = extractLocation(child);
    // handle multi-edges, i.e. chains of edges that have only one succeeding CFA-location.
    while (parentLoc.getNumLeavingEdges() == 1) {
      CFAEdge nextEdge = parentLoc.getLeavingEdge(0);
      CFANode nextLoc = nextEdge.getSuccessor();
      newFullPath.add(nextEdge);
      if (Objects.equals(nextLoc, childLoc)) {
        return; // child found -> finished
      }
      parentLoc = nextLoc;
    }
    // handle last edge of chain, we need to handle multiple successor nodes here.
    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(parentLoc)) {
      if (Objects.equals(leavingEdge.getSuccessor(), childLoc)) {
        newFullPath.add(leavingEdge);
        return; // child found -> finished
      }
    }
    throw new AssertionError("unexpected CFA locations");
  }

  private List<CFANode> getLocations() {
    return Lists.transform(asStatesList(), AbstractStates.EXTRACT_LOCATION);
  }

  @Override
  public int hashCode() {
    // We do not have edges for most SSCPaths, lets use locations from states.
    return Objects.hash(reachedSet, getLocations());
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (!(pOther instanceof SSCPath)) {
      return false;
    }
    // We do not compare the states because they are different from iteration to iteration!
    // We do not have edges for most SSCPaths, lets use locations from states.
    return super.equals(pOther)
        && Objects.equals(getLocations(), ((SSCPath) pOther).getLocations());
  }

  @Override
  public String toString() {
    return "SCCPath {" + Joiner.on("\n").join(asStatesList()) + "}";
  }
}
