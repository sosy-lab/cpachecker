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
package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * ARGPath contains a non-empty path through the ARG
 * consisting of both a sequence of states
 * and the edges between them.
 * Very often, the first state is the root state of the ARG,
 * and the last state is a target state, though this is not guaranteed.
 *
 * The number of states and edges is currently always equal.
 * To achieve this, the last edge is an outgoing edge of the location of the last state.
 *
 * States on this path cannot be null.
 * Edges can be null,
 * if there is no corresponding CFAEdge between two consecutive abstract states.
 */
@Immutable
public class ARGPath implements Appender, Iterable<Pair<ARGState, CFAEdge>> {

  private final ImmutableList<ARGState> states;
  private final List<CFAEdge> edges; // immutable, but may contain null

  ARGPath(List<ARGState> pStates) {
    checkArgument(!pStates.isEmpty(), "ARGPaths may not be empty");
    states = ImmutableList.copyOf(pStates);

    List<CFAEdge> edgesBuilder = new ArrayList<>(states.size());
    for (int i = 0; i < states.size() - 1; i++) {
      ARGState parent = states.get(i);
      ARGState child = states.get(i+1);
      edgesBuilder.add(parent.getEdgeToChild(child)); // may return null
    }

    // For backwards compatibility,
    // the list of states and edges should have same length.
    // For this, we add one outgoing edge of the last state to the list.
    CFANode lastLoc = extractLocation(states.get(states.size()-1));
    edgesBuilder.add(leavingEdges(lastLoc).first().orNull());

    edges = Collections.unmodifiableList(edgesBuilder);
    assert states.size() == edges.size();
  }

  public ARGPath(List<ARGState> pStates, List<CFAEdge> pEdges) {
    checkArgument(!pStates.isEmpty(), "ARGPaths may not be empty");
    checkArgument(pStates.size() == pEdges.size(), "ARGPaths must have equal number of states and edges");
    CFAEdge lastEdge = pEdges.get(pEdges.size()-1);
    if (lastEdge != null) {
      CFANode lastLoc = extractLocation(pStates.get(pStates.size()-1));
      checkArgument(leavingEdges(lastLoc).contains(lastEdge));
    }

    states = ImmutableList.copyOf(pStates);
    edges = Collections.unmodifiableList(new ArrayList<>(pEdges));
  }

  private ARGPath(ImmutableList<ARGState> pStates, List<CFAEdge> pEdges,
      int fromIndex, int toIndex) {
    states = pStates.subList(fromIndex, toIndex);
    edges = pEdges.subList(fromIndex, toIndex);
    checkArgument(!states.isEmpty(), "ARGPaths may not be empty");
  }

  public ImmutableList<ARGState> asStatesList() {
    return states;
  }

  public List<CFAEdge> asEdgesList() {
    return edges;
  }

  public ImmutableSet<ARGState> getStateSet() {
    return ImmutableSet.copyOf(states);
  }

  public MutableARGPath mutableCopy() {
    MutableARGPath result = new MutableARGPath();
    Iterators.addAll(result, iterator());
    return result;
  }

  @Override
  public Iterator<Pair<ARGState, CFAEdge>> iterator() {
    return Pair.zipWithPadding(states, edges).iterator();
  }

  public Iterator<Pair<ARGState, CFAEdge>> descendingIterator() {
    return Pair.zipWithPadding(states.reverse(), Lists.reverse(edges)).iterator();
  }

  public int size() {
    return states.size();
  }

  public Pair<ARGState, CFAEdge> get(int i) {
    return Pair.of(states.get(i), edges.get(i));
  }

  public Pair<ARGState, CFAEdge> getFirst() {
    return Pair.of(states.get(0), edges.get(0));
  }

  public Pair<ARGState, CFAEdge> getLast() {
    return Pair.of(states.get(size()-1), edges.get(size()-1));
  }

  /**
   * Return a snippet of this path.
   * @see List#subList(int, int)
     * @param fromIndex low endpoint (inclusive) of the subPath
     * @param toIndex high endpoint (exclusive) of the subPath
   * @return
   */
  public ARGPath subPath(int fromIndex, int toIndex) {
    return new ARGPath(states, edges, fromIndex, toIndex);
  }

  @Override
  public void appendTo(Appendable appendable) throws IOException {
    Joiner.on('\n').skipNulls().appendTo(appendable, asEdgesList());
  }

  @Override
  public String toString() {
    return Joiner.on('\n').skipNulls().join(asEdgesList());
  }

  public void toJSON(Appendable sb) throws IOException {
    List<Map<?, ?>> path = new ArrayList<>(size());
    for (Pair<ARGState, CFAEdge> pair : this) {
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
      path.add(elem);
    }
    JSON.writeJSONString(path, sb);
  }
}
