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
package org.sosy_lab.cpachecker.core.counterexample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.MultiConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.SingleConcreteState;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;

import com.google.common.collect.ImmutableList;


/**
 * This class represents a path of cfaEdges, that contain the additional Information
 * at which edge which assignableTerm was created when this path was checked by
 * the class {@link PathChecker}.
 *
 */
public class CFAPathWithAssumptions implements Iterable<CFAEdgeWithAssumptions> {

  private final List<CFAEdgeWithAssumptions> pathWithAssignments;

  private CFAPathWithAssumptions(
      List<CFAEdgeWithAssumptions> pPathWithAssignments) {
    pathWithAssignments = ImmutableList.copyOf(pPathWithAssignments);
  }

  private CFAPathWithAssumptions(
      CFAPathWithAssumptions pPathWithAssignments, CFAPathWithAssumptions pPathWithAssignments2) {

    assert pPathWithAssignments.size() == pPathWithAssignments2.size();

    List<CFAEdgeWithAssumptions> result = new ArrayList<>(pPathWithAssignments.size());
    Iterator<CFAEdgeWithAssumptions> path2Iterator = pPathWithAssignments2.iterator();

    for (CFAEdgeWithAssumptions edge : pPathWithAssignments) {
      CFAEdgeWithAssumptions resultEdge = edge.mergeEdge(path2Iterator.next());
      result.add(resultEdge);
    }

    pathWithAssignments = result;
  }

  public static CFAPathWithAssumptions empty() {
    return new CFAPathWithAssumptions(ImmutableList.<CFAEdgeWithAssumptions>of());
  }

  public ImmutableList<CFAEdgeWithAssumptions> asList() {
    return ImmutableList.copyOf(pathWithAssignments);
  }

  boolean fitsPath(List<CFAEdge> pPath) {

    if (pPath.size() != pathWithAssignments.size()) {
      return false;
    }

    int index = 0;

    for (CFAEdge edge : pPath) {

      CFAEdgeWithAssumptions cfaWithAssignment = pathWithAssignments.get(index);

      if (!edge.equals(cfaWithAssignment.getCFAEdge())) {
        return false;
      }

      index++;
    }

    return true;
  }

  @Nullable
  public Map<ARGState, CFAEdgeWithAssumptions> getExactVariableValues(ARGPath pPath) {


    if (pPath.getInnerEdges().size() != (pathWithAssignments.size())) {
      return null;
    }

    Map<ARGState, CFAEdgeWithAssumptions> result = new HashMap<>();

    PathIterator pathIterator = pPath.pathIterator();
    while (pathIterator.hasNext()) {

      CFAEdgeWithAssumptions edgeWithAssignment = pathWithAssignments.get(pathIterator.getIndex());
      CFAEdge argPathEdge = pathIterator.getOutgoingEdge();
      if (!edgeWithAssignment.getCFAEdge().equals(argPathEdge)) {
        // path is not equivalent
        return null;
      }

      result.put(pathIterator.getAbstractState(), edgeWithAssignment);
      pathIterator.advance();
    }
    // last state is ignored

    return result;
  }

  public static CFAPathWithAssumptions of(ConcreteStatePath statePath,
      AssumptionToEdgeAllocator pAllocator) {

    List<CFAEdgeWithAssumptions> result = new ArrayList<>(statePath.size());

    for (ConcreteStatePathNode node : statePath) {
      if (node instanceof SingleConcreteState) {

        SingleConcreteState singleState = (SingleConcreteState) node;
        CFAEdgeWithAssumptions edge = createCFAEdgeWithAssignment(singleState, pAllocator);
        result.add(edge);
      } else {
        MultiConcreteState multiState = (MultiConcreteState) node;
        CFAEdgeWithAssumptions edge = createCFAEdgeWithAssignment(multiState, pAllocator);
        result.add(edge);
      }
    }

    return new CFAPathWithAssumptions(result);
  }

  private static CFAEdgeWithAssumptions createCFAEdgeWithAssignment(MultiConcreteState state,
      AssumptionToEdgeAllocator pAllocator) {

    MultiEdge multiEdge = state.getCfaEdge();
    int sizeOfMultiEdge= multiEdge.getEdges().size();
    List<CFAEdgeWithAssumptions> edges = new ArrayList<>(sizeOfMultiEdge);


    // First, create all assumptions for each edge in the multi edge except the last one
    Iterator<SingleConcreteState> it = state.iterator();
    for (int c = 0; c < sizeOfMultiEdge - 1; c++) {
      SingleConcreteState node = it.next();
      edges.add(createCFAEdgeWithAssignment(node, pAllocator));
    }

    /* Second, create all assumptions at the end of the multi edge for
    * all changed variables. Since it is impossible to properly project
    * the assumption from the assumptions of the edges in the multi edge,
    * due to aliasing, simply create assumptions for all edges with the concrete state
    * of the last edge, thus correctly projecting all lvalues at the end of the multi edge.*/
    Set<AExpressionStatement> assumptions = new HashSet<>();
    Set<String> assumptionCodes = new HashSet<>();
    ConcreteState lastState = state.getLastConcreteState().getConcreteState();

    StringBuilder comment = new StringBuilder("");

    for (CFAEdge cfaEdge : multiEdge) {
      CFAEdgeWithAssumptions assumptionForedge = pAllocator.allocateAssumptionsToEdge(cfaEdge, lastState);

      // throw away redundant assumptions
      for (AExpressionStatement assumption : assumptionForedge.getExpStmts()) {
        if (!assumptionCodes.contains(assumption.toASTString())) {
          assumptions.add(assumption);
          assumptionCodes.add(assumption.toASTString());
        }
      }

      String commentOfEdge = assumptionForedge.getComment();

      if(commentOfEdge != null && !commentOfEdge.isEmpty()) {
        comment.append(commentOfEdge);
        comment.append("\n");
      }
    }

    // Finally create Last edge and multi edge
    ArrayList<AExpressionStatement> assumptionsList = new ArrayList<>(assumptions);
    CFAEdge lastEdge = state.getLastConcreteState().getCfaEdge();
    CFAEdgeWithAssumptions lastAssumptionEdge = new CFAEdgeWithAssumptions(lastEdge, assumptionsList, comment.toString());
    edges.add(lastAssumptionEdge);

    CFAMultiEdgeWithAssumptions edge = CFAMultiEdgeWithAssumptions.valueOf(multiEdge, edges, assumptionsList, comment.toString());
    return edge;
  }

  private static CFAEdgeWithAssumptions createCFAEdgeWithAssignment(
      SingleConcreteState pState, AssumptionToEdgeAllocator pAllocator) {

    CFAEdge cfaEdge = pState.getCfaEdge();
    ConcreteState concreteState = pState.getConcreteState();

    return pAllocator.allocateAssumptionsToEdge(cfaEdge, concreteState);
  }

  public boolean isEmpty() {
    return pathWithAssignments.isEmpty();
  }

  @Override
  public String toString() {
    return pathWithAssignments.toString();
  }

  public CFAEdge getCFAEdgeAtPosition(int index) {
    return pathWithAssignments.get(index).getCFAEdge();
  }

  public int size() {
    return pathWithAssignments.size();
  }

  @Override
  public Iterator<CFAEdgeWithAssumptions> iterator() {
    return pathWithAssignments.iterator();
  }

  public CFAPathWithAssumptions mergePaths(CFAPathWithAssumptions pOtherPath) {

    if (pOtherPath.size() != this.size()) {
      return this;
    }

    return new CFAPathWithAssumptions(this, pOtherPath);
  }
}