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
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocations;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * ARGPath contains a non-empty path through the ARG
 * consisting of both a sequence of states
 * and the edges between them.
 * Very often, the first state is the root state of the ARG,
 * and the last state is a target state, though this is not guaranteed.
 *
 * The number of states is always one larger than the number of edges.
 *
 * States on this path cannot be null.
 * Edges can be null,
 * if there is no corresponding CFAEdge between two consecutive abstract states.
 *
 * The recommended way to iterate through an ARGPath if you need both states and edges
 * is to use {@link #pathIterator()}.
 *
 * The usual way to get an ARGPath instance is from methods in {@link ARGUtils}
 * such as {@link ARGUtils#getOnePathTo(ARGState)} and {@link ARGUtils#getRandomPath(ARGState)}.
 */
@Immutable
public class ARGPath extends AbstractAppender {

  private final ImmutableList<ARGState> states;
  private final List<CFAEdge> edges; // immutable, but may contain null

  @SuppressFBWarnings(
      value="JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS",
      justification="This variable is only used for caching the full path for later use"
          + " without having to compute it again.")
  private List<CFAEdge> fullPath = null;

  protected ARGPath(ARGPath pArgPath) {
    states = pArgPath.states;
    edges = pArgPath.edges;
  }

  ARGPath(List<ARGState> pStates) {
    checkArgument(!pStates.isEmpty(), "ARGPaths may not be empty");
    states = ImmutableList.copyOf(pStates);

    List<CFAEdge> edgesBuilder = new ArrayList<>(states.size()-1);
    for (int i = 0; i < states.size() - 1; i++) {
      ARGState parent = states.get(i);
      ARGState child = states.get(i+1);
      edgesBuilder.add(parent.getEdgeToChild(child)); // may return null
    }

    edges = Collections.unmodifiableList(edgesBuilder);
    assert states.size() - 1 == edges.size();
  }

  public ARGPath(List<ARGState> pStates, List<CFAEdge> pEdges) {
    checkArgument(!pStates.isEmpty(), "ARGPaths may not be empty");
    checkArgument(pStates.size() - 1 == pEdges.size(), "ARGPaths must have one state more than edges");

    states = ImmutableList.copyOf(pStates);
    edges = Collections.unmodifiableList(new ArrayList<>(pEdges));
  }

  public ImmutableList<ARGState> asStatesList() {
    return states;
  }

  /**
   * Return the list of edges between the states.
   * The result of this method is always one element shorter
   * than {@link #asStatesList()}.
   */
  public List<CFAEdge> getInnerEdges() {
    return edges;
  }

  /**
   * Returns the full path contained in this {@link ARGPath}. This means, edges
   * which are null while using getInnerEdges or the pathIterator will be resolved
   * and the complete path from the first {@link ARGState} to the last ARGState
   * is created. This is done by filling up the wholes in the path.
   *
   * If there is no path (null edges can not be filled up, may be happening when
   * using bam) we return an empty list instead.
   */
  public List<CFAEdge> getFullPath() {
    if (fullPath != null) {
      return fullPath;
    }

    List<CFAEdge> fullPath = new ArrayList<>();
    PathIterator it = pathIterator();

    while (it.hasNext()) {
      ARGState prev = it.getAbstractState();
      CFAEdge curOutgoingEdge = it.getOutgoingEdge();
      it.advance();
      ARGState succ = it.getAbstractState();

      // assert prev.getEdgeToChild(succ) == curOutgoingEdge : "invalid ARGPath";

      // compute path between cur and next node
      if (curOutgoingEdge == null) {
        // we assume a linear chain of edges from 'prev' to 'succ'
        CFANode curNode = extractLocation(prev);
        CFANode nextNode = extractLocation(succ);

        do { // the chain must not be empty
          if (!(curNode.getNumLeavingEdges() == 1 && curNode.getLeavingSummaryEdge() == null)) {
            return Collections.emptyList();
          }

          CFAEdge intermediateEdge = curNode.getLeavingEdge(0);
          fullPath.add(intermediateEdge);
          curNode = intermediateEdge.getSuccessor();
        } while (curNode != nextNode);

      // we have a normal connection without hole in the edges
      } else {
        fullPath.add(curOutgoingEdge);
      }
    }

    this.fullPath = fullPath;
    return fullPath;
  }

  public ImmutableSet<ARGState> getStateSet() {
    return ImmutableSet.copyOf(states);
  }

  /**
   * Return (predecessor,successor) pairs of ARGStates for every edge in the path.
   */
  public List<Pair<ARGState, ARGState>> getStatePairs() {
    return new AbstractList<Pair<ARGState, ARGState>>() {

      @Override
      public Pair<ARGState, ARGState> get(int pIndex) {
        return Pair.of(states.get(pIndex), states.get(pIndex+1));
      }

      @Override
      public int size() {
        return states.size() - 1;
      }
    };
  }

  /**
   * Create a fresh {@link PathIterator} for this path,
   * with its position at the first state.
   * Note that you cannot call {@link PathIterator#getIncomingEdge()} before calling
   * {@link PathIterator#advance()} at least once.
   */
  public PathIterator pathIterator() {
    return new DefaultPathIterator(this);
  }

  /**
   * Create a fresh {@link PathIterator} for this path,
   * with its position at the last state and iterating backwards.
   * Note that you cannot call {@link PathIterator#getOutgoingEdge()} before calling
   * {@link PathIterator#advance()} at least once.
   */
  public PathIterator reversePathIterator() {
    return new ReversePathIterator(this);
  }

  /**
   * Create a fresh {@link PathIterator} for this path, with its position at the
   * first state. Holes in the path are filled up by inserting more {@link CFAEdge}.
   * Note that you cannot call {@link PathIterator#getIncomingEdge()} before calling
   * {@link PathIterator#advance()} at least once.
   */
  public PathIterator fullPathIterator() {
    return new DefaultFullPathIterator(this);
  }

  /**
   * Create a fresh {@link PathIterator} for this path, with its position at the
   * last state and iterating backwards. Holes in the path are filled up by inserting
   * more {@link CFAEdge}.
   * Note that you cannot call {@link PathIterator#getOutgoingEdge()} before calling
   * {@link PathIterator#advance()} at least once.
   */
  public PathIterator reverseFullPathIterator() {
    return new ReverseFullPathIterator(this);
  }

  /**
   * A forward directed {@link ARGPathBuilder} with no initial states and edges
   * added. (States and edges are always appended to the end of the current path)
   */
  public static ARGPathBuilder builder() {
    return new DefaultARGPathBuilder();
  }

  /**
   * A backward directed {@link ARGPathBuilder} with no initial states and edges
   * added. (States and edges are always appended to the beginning of the current path)
   */
  public static ARGPathBuilder reverseBuilder() {
    return new ReverseARGPathBuilder();
  }

  /**
   * The length of the path, i.e., the number of states
   * (this is different from the number of edges).
   */
  public int size() {
    return states.size();
  }

  public ARGState getFirstState() {
    return states.get(0);
  }

  public ARGState getLastState() {
    return Iterables.getLast(states);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((edges == null) ? 0 : edges.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) { return true; }
    if (!(pOther instanceof ARGPath)) { return false; }

    ARGPath other = (ARGPath) pOther;

    if (edges == null) {
      if (other.edges != null) { return false; }
    } else if (!edges.equals(other.edges)) { return false; }

    // We do not compare the states because they are different from iteration to iteration!

    return true;
  }

  @Override
  public void appendTo(Appendable appendable) throws IOException {
    Joiner.on(System.lineSeparator()).skipNulls().appendTo(appendable, getFullPath());
    appendable.append(System.lineSeparator());
  }

  /**
   * A class for creating {@link ARGPath}s by iteratively adding path elements
   * one after another. ARGPaths can be built either from the beginning to the
   * endpoint or in reverse.
   * The builder can still be used after calling {@link #build(ARGState)}.
   * Please note that the state and edge given to the build method will not be
   * added permanently to the builder. If they should be in the builder afterwards
   * you need to use  {@link #add(ARGState, CFAEdge)}.
   *
   * In the future we want to remove the edge given to the build method. An
   * outgoing edge of the last state of a path does not make sense.
   */
  public static abstract class ARGPathBuilder {
    List<ARGState> states = new ArrayList<>();
    List<CFAEdge> edges = new ArrayList<>();

    private ARGPathBuilder() {}

    /**
     * Returns the amount of states which are currently added to this builder.
     */
    public int size() {
      return states.size();
    }

    /**
     * Add the given state and edge to the ARGPath that should be created.
     */
    public ARGPathBuilder add(ARGState state, CFAEdge outgoingEdge) {
      states.add(state);
      edges.add(outgoingEdge);
      return this;
    }

    /**
     * Remove the state and edge that were added at last.
     */
    public ARGPathBuilder removeLast() {
      assert !states.isEmpty() && !edges.isEmpty();
      states.remove(states.size()-1);
      edges.remove(edges.size()-1);
      return this;
    }

    /**
     * Build the ARGPath using the given state as the last state.
     */
    public abstract ARGPath build(ARGState state);
  }

  /**
   * The implementation of the ARGPathBuilder that adds new states and edges
   * at the end of the Path.
   */
  private static class DefaultARGPathBuilder extends ARGPathBuilder {

    @Override
    public ARGPath build(ARGState pState) {
      states.add(pState);
      ARGPath path = new ARGPath(states, edges);
      states.remove(states.size()-1);
      return path;
    }
  }

  /**
   * The implementation of the ARGPathBuilder that adds new states and edges
   * at the beginning of the Path.
   */
  private static class ReverseARGPathBuilder extends ARGPathBuilder {

    @Override
    public ARGPath build(ARGState pState) {
      states.add(pState);
      ARGPath path = new ARGPath(Lists.reverse(states), Lists.reverse(edges));
      states.remove(states.size()-1);
      return path;
    }
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
  public static abstract class PathIterator {

    protected int pos; // the index of the current stat
    protected final ARGPath path;

    private PathIterator(ARGPath pPath, int pPos) {
      this.path = pPath;
      this.pos = pPos;
    }

    /**
     * Check whether there is at least one more state in the path.
     */
    public abstract boolean hasNext();

    /**
     * Check whether there is at least one state before the current one in the path.
     */
    public abstract boolean hasPrevious();

    /**
     * Get the current position of the iterator
     * (first state is at position 0).
     */
    public int getIndex() {
      return pos;
    }

    /**
     * Get a {@link PathPosition} instance that refers to the current position of this iterator.
     * Can be used to create a new iterator in the same position.
     */
    public PathPosition getPosition() {
      return new PathPosition(path, getIndex());
    }

    /**
     * Advance the iterator by one position.
     * @throws IllegalStateException If {@link #hasNext()} would return false.
     */
    public abstract void advance() throws IllegalStateException;

    /**
     * Rewind the iterator by one position
     * @throws IllegalStateException if {@link #hasPrevious()} would return false.
     */
    public abstract void rewind() throws IllegalStateException;

    /**
     * Checks whether the iterator can be advanced and does so it it is possible.
     *
     * @return Indicates whether the iterator could be advanced or not
     */
    public boolean advanceIfPossible() {
      if (hasNext()) {
        advance();
        return true;
      } else {
        return false;
      }
    }

    public boolean rewindIfPossible() {
      if (hasPrevious()) {
        rewind();
        return true;
      } else {
        return false;
      }
    }

    /**
     * Indicates whether the current position of this iterator has a state.
     * For {@code ARGPath#pathIterator()} and {@code ARGPath#reversePathIterator()}
     * this will always return <code>true</code>. For other iterators, e.g. the
     * {@link FullPathIterator} there may be holes in the iterated path, as the
     * edges are expanded to the full path (and therefore they do not have holes
     * anymore).
     */
    public boolean isPositionWithState() {
      return true;
    }

    /**
     * Get the abstract state at the current position.
     * Note that unlike {@link Iterator#next()}, this does not change the iterator's state.
     * @return A non-null {@link ARGState}.
     */
    public ARGState getAbstractState() {
      return path.states.get(pos);
    }

    /**
     * Get the abstract state at the next position.
     * Note that unlike {@link Iterator#next()}, this does not change the iterator's state.
     * May not be called when this iterator points to the last state in the path
     * (at the end of an iteration with a forwards PathIterator,
     * or at the beginning of an iteration with a backwards PathIterator).
     * @return A non-null {@link ARGState}.
     */
    public ARGState getNextAbstractState() {
      checkState(pos + 1 < path.states.size());
      return path.states.get(pos+1);
    }

    /**
     * Get the abstract state at the previous position.
     * Note that unlike {@link Iterator#next()}, this does not change the iterator's state.
     * May not be called when this iterator points to the first state in the path
     * (at the beginning of an iteration with a forwards PathIterator,
     * or at the end of an iteration with a backwards PathIterator).
     * @return A non-null {@link ARGState}.
     */
    public ARGState getPreviousAbstractState() {
      checkState(pos - 1 >= 0);
      return path.states.get(pos-1);
    }

    /**
     * Get the CFA location at the current position.
     *
     * Using the nodes of a CFAEdge might provide a wrong result
     *    in the case of a backwards analysis!!
     *
     * Note that unlike {@link Iterator#next()}, this does not change the iterator's state.
     * @return A non-null {@link CFANode}.
     */
    public CFANode getLocation() {
      return AbstractStates.extractLocation(getAbstractState());
    }

    /**
     * Get the edge before the current abstract state.
     * May not be called when this iterator points to the first state in the path
     * (at the beginning of an iteration with a forwards PathIterator,
     * or at the end of an iteration with a backwards PathIterator).
     * @return A {@link CFAEdge} or null, if there is no edge between these two states.
     */
    public @Nullable CFAEdge getIncomingEdge() {
      checkState(pos > 0, "First state in ARGPath has no incoming edge.");
      return path.edges.get(pos-1);
    }

    /**
     * Get the edge after the current abstract state.
     * May not be called when this iterator points to the last state in the path
     * (at the end of an iteration with a forwards PathIterator,
     * or at the beginning of an iteration with a backwards PathIterator).
     * @return A {@link CFAEdge} or null, if there is no edge between these two states.
     */
    public @Nullable CFAEdge getOutgoingEdge() {
      checkState(pos < path.states.size()-1, "Last state in ARGPath has no outgoing edge.");
      return path.edges.get(pos);
    }

    /**
     * Get the prefix of the current ARGPath from the first state to the current
     * state (inclusive) returned by this iterator.
     * The prefix will always be forwards directed, thus the {@link ReversePathIterator}
     * does also return the sequence from the first state of the ARGPath up (inclusive)
     * the current position of the iterator.
     *
     * @return A non-null {@link ARGPath}
     */
    public ARGPath getPrefixInclusive() {
      return new ARGPath(path.states.subList(0, pos+1), path.edges.subList(0, pos));
    }

    /**
     * Get the prefix of the current ARGPath from the first state to the current
     * state (exclusive) returned by this iterator.
     * The prefix will always be forwards directed, thus the {@link ReversePathIterator}
     * does also return the sequence from the first state of the ARGPath up (exclusive)
     * the current position of the iterator.
     *
     * May not be called when this iterator points to the first state in the path
     * (at the beginning of an iteration with a forwards PathIterator,
     * or at the end of an iteration with a backwards PathIterator).
     *
     * @return A non-null {@link ARGPath}
     */
    public ARGPath getPrefixExclusive() {
      checkState(pos > 0, "Exclusive prefix of first state in path would be empty.");

      if (pos == 1) {
        return new ARGPath(path.states.subList(0, pos), Collections.<CFAEdge>emptyList());
      } else {
        return new ARGPath(path.states.subList(0, pos), path.edges.subList(0, pos - 1));
      }
    }

    /**
     * Get the suffix of the current ARGPath from the current state (inclusive) to the
     * last state returned by this iterator.
     * The suffix will always be forwards directed, thus the {@link ReversePathIterator}
     * does also return the sequence from the current state of the ARGPath (inclusive)
     * up to the last position of the iterator.
     *
     * @return A non-null {@link ARGPath}
     */
    public ARGPath getSuffixInclusive() {
      int lastPos = path.states.size();
      return new ARGPath(path.states.subList(pos, lastPos), path.edges.subList(pos, lastPos-1));
    }

    /**
     * Get the suffix of the current ARGPath from the current state (exclusive) to the
     * last state returned by this iterator.
     * The suffix will always be forwards directed, thus the {@link ReversePathIterator}
     * does also return the sequence from the current state of the ARGPath (exclusive)
     * up to the last position of the iterator.
     *
     * May not be called when this iterator points to the last state in the path
     * (at the end of an iteration with a forwards PathIterator,
     * or at the beginning of an iteration with a backwards PathIterator).
     *
     * @return A non-null {@link ARGPath}
     */
    public ARGPath getSuffixExclusive() {
      checkState(pos < path.states.size() - 1, "Exclusive suffix of last state in path would be empty.");
      int lastPos = path.states.size();
      return new ARGPath(path.states.subList(pos+1, lastPos), path.edges.subList(pos+1, lastPos-1));
    }
  }

  /**
   * A marker for a specific position in an {@link ARGPath}.
   * This class is independent of the traversal order of the iterator that was used to create it.
   */
  public static class PathPosition {

    protected final int pos;
    protected final ARGPath path;

    private PathPosition(ARGPath pPath, int pPosition) {
      this.path = pPath;
      this.pos = pPosition;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + path.hashCode();
      result = prime * result + pos;
      return result;
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof PathPosition)) {
        return false;
      }
      PathPosition other = (PathPosition) pObj;

      return ((this.pos == other.pos)
          && (this.path.equals(other.path)));
    }

    /**
     * Create a fresh {@link PathIterator} for this path,
     * initialized at this position of the path, and iterating forwards.
     */
    public PathIterator iterator() {
      return new DefaultPathIterator(path, pos);
    }

    /**
     * Create a fresh {@link PathIterator} for this path,
     * initialized at this position of the path, and iterating backwards.
     */
    public PathIterator reverseIterator() {
      return new ReversePathIterator(path, pos);
    }

    /**
     * Create a fresh {@link FullPathIterator} for this path,
     * initialized at this position of the path, and iterating forwards.
     *
     * Note: if the  {@link PathPosition} object was not created from a
     * FullPathIterator the iteration will always start at a position with
     * abstract state, not inside and ARG hole.
     */
    public PathIterator fullPathIterator() {
      PathIterator it = new DefaultFullPathIterator(path);
      while (it.pos != pos) {
        it.advance();
      }
      assert it.pos == pos;

      return new DefaultFullPathIterator(path, pos, it.getIndex());
    }

    /**
     * Create a fresh {@link FullPathIterator} for this path,
     * initialized at this position of the path, and iterating backwards.
     *
     * Note: if the  {@link PathPosition} object was not created from a
     * FullPathIterator the iteration will always start at a position with
     * abstract state, not inside and ARG hole.
     */
    public PathIterator reverseFullPathIterator() {
      PathIterator it = new ReverseFullPathIterator(path);
      // get to the correct abstract state location
      while (it.pos != pos) {
        it.advance();
      }
      // now move until the offset is also correct
      while (it.hasNext() && it.pos == pos) {
        it.advance();
      }

      if (pos == it.pos) {
        assert pos == 0;
        return new ReverseFullPathIterator(path, pos, pos);
      } else {
        assert pos == it.pos + 1;
        return new ReverseFullPathIterator(path, pos, it.getIndex() + 1);
      }
    }

    /**
     * @see PathIterator#getLocation()
     */
    public CFANode getLocation() {
      return iterator().getLocation();
    }

    /**
     * Return the {@link ARGPath} that this position belongs to.
     */
    public ARGPath getPath() {
      return path;
    }
  }

  private static class FullPathPosition extends PathPosition {

    private final int offset;

    private FullPathPosition(ARGPath pPath, int pPosition, int pOffset) {
      super(pPath, pPosition);
      offset = pOffset;
    }

    /**
     * {@inheritDoc}
     * The position is exact, that means if the position is in the middle of an
     * ARG hole the iterator will start there.
     */
    @Override
    public PathIterator fullPathIterator() {
      return new DefaultFullPathIterator(path, pos, offset);
    }

    /**
     * {@inheritDoc}
     * The position is exact, that means if the position is in the middle of an
     * ARG hole the iterator will start there.
     */
    @Override
    public PathIterator reverseFullPathIterator() {
      return new ReverseFullPathIterator(path, pos, offset);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + path.hashCode();
      result = prime * result + pos;
      result = prime * result + offset;
      return result;
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof FullPathPosition)) {
        return false;
      }
      FullPathPosition other = (FullPathPosition) pObj;

      return super.equals(pObj) && other.offset == this.offset;
    }
  }

  /**
   * The implementation of PathIterator that iterates
   * in the direction of the analysis.
   */
  private static class DefaultPathIterator extends PathIterator {

    private DefaultPathIterator(ARGPath pPath, int pPos) {
      super(pPath, pPos);
    }

    private DefaultPathIterator(ARGPath pPath) {
      super(pPath, 0);
    }

    @Override
    public void advance() throws IllegalStateException {
      checkState(hasNext(), "No more states in PathIterator.");
      pos++;
    }

    @Override
    public void rewind() throws IllegalStateException {
      checkState(hasPrevious(), "No previous state in PathIterator.");
      pos--;
    }

    @Override
    public boolean hasNext() {
      return pos < path.states.size()-1;
    }

    @Override
    public boolean hasPrevious() {
      return pos > 0;
    }
  }
  /**
   * The implementation of PathIterator that iterates
   * in the reverse direction of the analysis.
   */
  private static class ReversePathIterator extends PathIterator {

    private ReversePathIterator(ARGPath pPath, int pPos) {
      super(pPath, pPos);
    }

    private ReversePathIterator(ARGPath pPath) {
      super(pPath, pPath.states.size()-1);
    }

    @Override
    public void advance() throws IllegalStateException {
      checkState(hasNext(), "No more states in PathIterator.");
      pos--;
    }

    @Override
    public void rewind() throws IllegalStateException {
      checkState(hasPrevious(), "No previous states in PathIterator.");
      pos++;
    }

    @Override
    public boolean hasNext() {
      return pos > 0;
    }

    @Override
    public boolean hasPrevious() {
      return pos < path.states.size() - 1;
    }
  }

  private static abstract class FullPathIterator extends PathIterator {
    protected final List<CFAEdge> fullPath;
    protected boolean currentPositionHasState = true;
    protected int overallOffset;

    private FullPathIterator(ARGPath pPath, int pPos, int pOverallOffset) {
      super(pPath, pPos);
      fullPath = pPath.getFullPath();
      overallOffset = pOverallOffset;
    }

    @Override
    public int getIndex() {
      return overallOffset;
    }

    @Override
    public PathPosition getPosition() {
      return new FullPathPosition(path, pos, overallOffset);
    }

    /**
     * {@inheritDoc}
     * May only be called on positions of the iterator where we have an {@link ARGState}
     * not in the edges that fill up holes between them.
     */
    @Override
    public ARGState getAbstractState() {
      checkState(currentPositionHasState);
      return path.states.get(pos);
    }

    @Override
    public boolean isPositionWithState() {
      return currentPositionHasState;
    }

    @Override
    public @Nullable CFAEdge getIncomingEdge() {
      checkState(pos > 0, "First state in ARGPath has no incoming edge.");
      return fullPath.get(overallOffset-1);
    }

    @Override
    public @Nullable CFAEdge getOutgoingEdge() {
      checkState(pos < path.states.size()-1, "Last state in ARGPath has no outgoing edge.");
      return fullPath.get(overallOffset);
    }

    /**
     * {@inheritDoc}
     * Returns the directly previous AbstractState that can be found, thus this is
     * the appropriate replacement for {@code FullPathIterator#getAbstractState()}
     * if the iterator is currently in a hole in the path that was filled with
     * additional edges.
     */
    @Override
    public ARGState getPreviousAbstractState() {
      checkState(pos - 1 >= 0);
      if (currentPositionHasState) {
        return path.states.get(pos-1);
      } else {
        return path.states.get(pos);
      }
    }

    /**
     * {@inheritDoc}
     * While in the hole of a path prefix inclusive returns the prefix inclusive
     * the state following the hole of this path.
     */
    @Override
    public ARGPath getPrefixInclusive() {
      if (currentPositionHasState) {
        return new ARGPath(path.states.subList(0, pos+1), path.edges.subList(0, pos));
      } else {
        return new ARGPath(path.states.subList(0, pos+2), path.edges.subList(0, pos+1));
      }
    }

    /**
     * {@inheritDoc}
     * While in the hole of a path prefix exclusive returns the prefix exclusive
     * the state following the hole of this path. (But inclusive the last found
     * state)
     */
    @Override
    public ARGPath getPrefixExclusive() {
      checkState(
          !currentPositionHasState || pos > 0,
          "Exclusive prefix of first state in path would be empty.");
      if (currentPositionHasState) {
        if (pos == 0) {
          return new ARGPath(path.states.subList(0, pos), Collections.<CFAEdge>emptyList());
        } else {
          return new ARGPath(path.states.subList(0, pos), path.edges.subList(0, pos - 1));
        }
      } else {
        return new ARGPath(path.states.subList(0, pos+1), path.edges.subList(0, pos));
      }
    }
  }

  private static class DefaultFullPathIterator extends FullPathIterator {

    private DefaultFullPathIterator(ARGPath pPath, int pPos, int pOverallOffset) {
      super(pPath, pPos, pOverallOffset);
    }

    private DefaultFullPathIterator(ARGPath pPath) {
      this(pPath, 0, 0);
    }

    @Override
    public void advance() throws IllegalStateException {
      checkState(hasNext(), "No more states in PathIterator.");

      CFAEdge nextEdge = fullPath.get(overallOffset);
      Iterable<CFANode> nextLocation = extractLocations(getNextAbstractState());
      boolean nextPositionHasState =
          Iterables.contains(nextLocation, nextEdge.getSuccessor()) // forward analysis
              || Iterables.contains(nextLocation, nextEdge.getPredecessor()); // backward analysis

      if (nextPositionHasState) {
        pos++;
      }

      currentPositionHasState = nextPositionHasState;
      overallOffset++;
    }

    @Override
    public void rewind() throws IllegalStateException {
      checkState(hasPrevious(), "No more states in PathIterator.");

      boolean previousPositionHasState =
          Iterables.contains(
              extractLocations(getPreviousAbstractState()),
              fullPath.get(overallOffset - 1).getPredecessor());

      if (currentPositionHasState) {
        pos--; // only reduce by one if it was a real node before we are leaving it now
      }

      currentPositionHasState = previousPositionHasState;

      overallOffset--;
    }

    @Override
    public boolean hasNext() {
      return pos < path.states.size()-1;
    }

    @Override
    public boolean hasPrevious() {
      return overallOffset > 0;
    }
  }

  private static class ReverseFullPathIterator extends FullPathIterator {

    private ReverseFullPathIterator(ARGPath pPath, int pPos, int pOverallOffset) {
      super(pPath, pPos, pOverallOffset);
    }

    private ReverseFullPathIterator(ARGPath pPath) {
      this(pPath, pPath.states.size() - 1, pPath.getFullPath().size());
    }

    @Override
    public void advance() throws IllegalStateException {
      checkState(hasNext(), "No more states in PathIterator.");

      // if we are currently on a position with state and we have a real
      // (non-null) edge then we can directly set the parameters without
      // further checking
      if (path.edges.get(pos-1) != null && currentPositionHasState) {
        pos--;
        overallOffset--;
        currentPositionHasState = true;

      } else {

        boolean nextPositionHasState = Iterables.contains(
            extractLocations(getPreviousAbstractState()),
            fullPath.get(overallOffset-1).getPredecessor());

        if (currentPositionHasState) {
          pos--; // only reduce by one if it was a real node before we are leaving it now
        }

        currentPositionHasState = nextPositionHasState;

        overallOffset--;
      }
    }

    @Override
    public void rewind() throws IllegalStateException {
      checkState(hasPrevious(), "No more states in PathIterator.");

      // if we are currently on a position with state and we have a real
      // (non-null) edge then we can directly set the parameters without
      // further checking
      if (path.edges.get(pos) != null && currentPositionHasState) {
        pos++;
        overallOffset++;
        currentPositionHasState = true;

      } else {
        if (Iterables.contains(
            extractLocations(getNextAbstractState()), fullPath.get(overallOffset).getSuccessor())) {
          pos++;
          currentPositionHasState = true;
        } else {
          currentPositionHasState = false;
        }
        overallOffset++;
      }
    }

    @Override
    public boolean hasNext() {
      return overallOffset > 0;
    }

    @Override
    public boolean hasPrevious() {
      return pos < path.states.size() - 1;
    }
  }
}
