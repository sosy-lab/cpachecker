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

import static com.google.common.base.Preconditions.*;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * ARGPath contains a non-empty path through the ARG
 * consisting of both a sequence of states
 * and the edges between them.
 * Very often, the first state is the root state of the ARG,
 * and the last state is a target state, though this is not guaranteed.
 *
 * The number of states and edges is currently always equal.
 * To achieve this, the last edge is an outgoing edge of the location of the last state.
 * If you want only the edges up to the last state and not beyond,
 * use {@link #getInnerEdges()} or {@link #pathIterator()}
 * instead of {@link #asEdgesList()} (this is recommended).
 *
 * States on this path cannot be null.
 * Edges can be null,
 * if there is no corresponding CFAEdge between two consecutive abstract states.
 *
 * The recommended way to iterate through an ARGPath if you need both states and edges
 * is to use {@link #pathIterator()}.
 */
@Immutable
public class ARGPath implements Appender {

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

  public ImmutableList<ARGState> asStatesList() {
    return states;
  }

  public List<CFAEdge> asEdgesList() {
    return edges;
  }

  /**
   * Only return the list of edges between the states,
   * excluding the one edge after the last state.
   * The result of this method is always one element shorter
   * than {@link #asEdgesList()}.
   */
  public List<CFAEdge> getInnerEdges() {
    return edges.subList(0, edges.size()-1);
  }

  public ImmutableSet<ARGState> getStateSet() {
    return ImmutableSet.copyOf(states);
  }

  public MutableARGPath mutableCopy() {
    MutableARGPath result = new MutableARGPath();
    Iterables.addAll(result, Pair.zipWithPadding(states, edges));
    return result;
  }

  /**
   * Create a fresh {@link PathIterator} for this path,
   * with its position at the first state.
   */
  public PathIterator pathIterator() {
    return new PathIterator();
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

  @Override
  public void appendTo(Appendable appendable) throws IOException {
    Joiner.on('\n').skipNulls().appendTo(appendable, getInnerEdges());
  }

  @Override
  public String toString() {
    return Joiner.on('\n').skipNulls().join(getInnerEdges());
  }

  public void toJSON(Appendable sb) throws IOException {
    List<Map<?, ?>> path = new ArrayList<>(size());
    for (Pair<ARGState, CFAEdge> pair : Pair.zipWithPadding(states, edges)) {
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

  /**
   * An {@link Iterator}-like class for iterating through an {@link ARGPath}
   * providing access to both the abstract states and the edges.
   * The iterator's position is always at an abstract state,
   * and from this position allows access to the abstract state
   * and the edges before and after this state.
   *
   * A typical use case would look like this:
   * <code>
   * PathIterator it = path.pathIterator();
   * while (it.hasNext()) {
   *   handleState(it.getAbstractState());
   *   if (it.hasNext()) {
   *     handleEdge(it.getOutgoingEdge());
   *   }
   * }
   * </code>
   *
   * or like this:
   * <code>
   * PathIterator it = path.pathIterator();
   * handleFirstState(it.getAbstractState()); // safe because paths are never empty
   * while (it.hasNext()) {
   *   handleEdge(it.getIncomingEdge());
   *   handleState(it.getAbstractState());
   * }
   * </code>
   */
  public class PathIterator {

    private int pos = 0; // the index of the current stat

    /**
     * Check whether there is at least one more state in the path.
     */
    public boolean hasNext() {
      return pos < states.size()-1;
    }

    /**
     * Advance the iterator by one position.
     * @throws IllegalStateException If {@link #hasNext()} would return false.
     */
    public void advance() throws IllegalStateException {
      checkState(hasNext(), "No more states in PathIterator.");
      pos++;
    }

    /**
     * Get the current position of the iterator
     * (first state is at position 0).
     */
    public int getIndex() {
      return pos;
    }

    /**
     * Get the abstract state at the current position.
     * Note that unlike {@link Iterator#next()}, this does not change the iterator's state.
     * @return A non-null {@link ARGState}.
     */
    public ARGState getAbstractState() {
      return states.get(pos);
    }

    /**
     * Get the edge before the current abstract state.
     * May not be called before {@link #advance()} was called once
     * (there is no edge before the first state).
     * @return A {@link CFAEdge} or null, if there is no edge between these two states.
     */
    public @Nullable CFAEdge getIncomingEdge() {
      checkState(pos > 0, "First state in ARGPath has no incoming edge.");
      return edges.get(pos-1);
    }

    /**
     * Get the edge after the current abstract state.
     * May not be called when {@link #hasNext()} would return false
     * (there is no edge after the last state).
     * @return A {@link CFAEdge} or null, if there is no edge between these two states.
     */
    public @Nullable CFAEdge getOutgoingEdge() {
      checkState(hasNext(), "Last state in ARGPath has no outgoing edge.");
      return edges.get(pos);
    }
  }
}
