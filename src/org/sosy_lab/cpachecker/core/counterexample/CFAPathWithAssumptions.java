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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.common.JSON;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcerteStatePathNode;
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

  public CFAPathWithAssumptions() {
    pathWithAssignments = ImmutableList.of();
  }

  @Nullable
  public CFAPathWithAssumptions getExactVariableValues(List<CFAEdge> pPath) {

    if (fitsPath(pPath)) {
      return this;
    }

    int index = pathWithAssignments.size() - pPath.size();

    if (index < 0) {
      return null;
    }

    List<CFAEdgeWithAssumptions> result;

    result = new ArrayList<>(pPath.size());

    for (CFAEdge edge : pPath) {

      if (index > pathWithAssignments.size()) {
        return null;
      }

      CFAEdgeWithAssumptions cfaWithAssignment = pathWithAssignments.get(index);

      if (!edge.equals(cfaWithAssignment.getCFAEdge())) {
        return null;
      }

      result.add(cfaWithAssignment);
      index++;
    }

    return new CFAPathWithAssumptions(result);
  }

  private boolean fitsPath(List<CFAEdge> pPath) {

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

    for (ConcerteStatePathNode node : statePath) {
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

    MultiEdge cfaEdge = state.getCfaEdge();
    List<CFAEdgeWithAssumptions> pEdges = new ArrayList<>(cfaEdge.getEdges().size());

    for (SingleConcreteState node : state) {
      pEdges.add(createCFAEdgeWithAssignment(node, pAllocator));
    }

    CFAMultiEdgeWithAssumptions edge = CFAMultiEdgeWithAssumptions.valueOf(cfaEdge, pEdges);
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

  public void toJSON(Appendable sb, ARGPath argPath) throws IOException {
    List<Map<?, ?>> path = new ArrayList<>(this.size());

    if (argPath.getInnerEdges().size() != pathWithAssignments.size()) {
      argPath.toJSON(sb);
      return;
    }

    int index = 0;

    for (Pair<ARGState, CFAEdge> pair : Pair.zipWithPadding(argPath.asStatesList(), argPath.asEdgesList())) {

      Map<String, Object> elem = new HashMap<>();

      ARGState argelem = pair.getFirst();
      CFAEdge edge = pair.getSecond();

      if (edge == null) {
        continue; // in this case we do not need the edge
      }

      elem.put("argelem", argelem.getStateId());
      elem.put("source", edge.getPredecessor().getNodeNumber());
      elem.put("target", edge.getSuccessor().getNodeNumber());
      elem.put("desc", edge.getDescription().replaceAll("\n", " "));
      elem.put("line", edge.getFileLocation().getStartingLineNumber());
      elem.put("file", edge.getFileLocation().getFileName());

      // cfa path with assignments has no padding (only inner edges of argpath).
      if (index == pathWithAssignments.size()) {
        elem.put("val", "");
      } else {
        CFAEdgeWithAssumptions edgeWithAssignment = pathWithAssignments.get(index);
        elem.put("val", edgeWithAssignment.printForHTML());
      }

      path.add(elem);
      index++;
    }

    JSON.writeJSONString(path, sb);
  }

  public CFAPathWithAssumptions mergePaths(CFAPathWithAssumptions pOtherPath) {

    if (pOtherPath.size() != this.size()) {
      return this;
    }

    return new CFAPathWithAssumptions(this, pOtherPath);
  }
}