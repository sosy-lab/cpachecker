// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.path;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * An {@link Iterator}-like class for iterating through an {@link ARGPath} providing access to both
 * the abstract states and the edges. The iterator's position is always at an abstract state, and
 * from this position allows access to the abstract state and the edges before and after this state.
 *
 * <p>A typical use case would look like this: <code>
 * PathIterator it = path.pathIterator();
 * while (it.hasNext()) {
 *   handleState(it.getAbstractState());
 *   if (it.hasNext()) {
 *     handleEdge(it.getOutgoingEdge());
 *   }
 * }
 * </code> or like this: <code>
 * PathIterator it = path.pathIterator();
 * handleFirstState(it.getAbstractState()); // safe because paths are never empty
 * while (it.hasNext()) {
 *   handleEdge(it.getIncomingEdge());
 *   handleState(it.getAbstractState());
 * }
 * </code>
 */
public abstract class PathIterator {

  protected int pos; // the index of the current stat
  protected final ARGPath path;

  PathIterator(ARGPath pPath, int pPos) {
    path = pPath;
    pos = pPos;
  }

  /** Check whether there is at least one more state in the path. */
  public abstract boolean hasNext();

  /** Check whether there is at least one state before the current one in the path. */
  public abstract boolean hasPrevious();

  /** Get the current position of the iterator (first state is at position 0). */
  public int getIndex() {
    return pos;
  }

  /**
   * Get a {@link PathPosition} instance that refers to the current position of this iterator. Can
   * be used to create a new iterator in the same position.
   */
  public PathPosition getPosition() {
    return new PathPosition(path, getIndex());
  }

  /**
   * Advance the iterator by one position.
   *
   * @throws IllegalStateException If {@link #hasNext()} would return false.
   */
  public abstract void advance() throws IllegalStateException;

  /**
   * Rewind the iterator by one position
   *
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
   * Indicates whether the current position of this iterator has a state. For {@code
   * ARGPath#pathIterator()} and {@code ARGPath#reversePathIterator()} this will always return
   * <code>true</code>. For other iterators, e.g. the {@link FullPathIterator} there may be holes in
   * the iterated path, as the edges are expanded to the full path (and therefore they do not have
   * holes anymore).
   */
  public boolean isPositionWithState() {
    return true;
  }

  /**
   * Get the abstract state at the current position. Note that unlike {@link Iterator#next()}, this
   * does not change the iterator's state.
   *
   * @return A non-null {@link ARGState}.
   */
  public ARGState getAbstractState() {
    return path.asStatesList().get(pos);
  }

  /**
   * Get the abstract state at the next position. Note that unlike {@link Iterator#next()}, this
   * does not change the iterator's state. May not be called when this iterator points to the last
   * state in the path (at the end of an iteration with a forwards PathIterator, or at the beginning
   * of an iteration with a backwards PathIterator).
   *
   * @return A non-null {@link ARGState}.
   */
  public ARGState getNextAbstractState() {
    checkState(pos + 1 < path.size());
    return path.asStatesList().get(pos + 1);
  }

  /**
   * Get the abstract state at the previous position. Note that unlike {@link Iterator#next()}, this
   * does not change the iterator's state. May not be called when this iterator points to the first
   * state in the path (at the beginning of an iteration with a forwards PathIterator, or at the end
   * of an iteration with a backwards PathIterator).
   *
   * @return A non-null {@link ARGState}.
   */
  public ARGState getPreviousAbstractState() {
    checkState(pos - 1 >= 0);
    return path.asStatesList().get(pos - 1);
  }

  /**
   * Get the CFA location at the current position.
   *
   * <p>Using the nodes of a CFAEdge might provide a wrong result in the case of a backwards
   * analysis!!
   *
   * <p>Note that unlike {@link Iterator#next()}, this does not change the iterator's state.
   *
   * @return A non-null {@link CFANode}.
   */
  public CFANode getLocation() {
    return AbstractStates.extractLocation(getAbstractState());
  }

  /**
   * Get the edge before the current abstract state. May not be called when this iterator points to
   * the first state in the path (at the beginning of an iteration with a forwards PathIterator, or
   * at the end of an iteration with a backwards PathIterator).
   *
   * @return A {@link CFAEdge} or null, if there is no edge between these two states.
   */
  public @Nullable CFAEdge getIncomingEdge() {
    checkState(pos > 0, "First state in ARGPath has no incoming edge.");
    return path.getInnerEdges().get(pos - 1);
  }

  /**
   * Get the edge after the current abstract state. May not be called when this iterator points to
   * the last state in the path (at the end of an iteration with a forwards PathIterator, or at the
   * beginning of an iteration with a backwards PathIterator).
   *
   * @return A {@link CFAEdge} or null, if there is no edge between these two states.
   */
  public @Nullable CFAEdge getOutgoingEdge() {
    checkState(pos < path.size() - 1, "Last state in ARGPath has no outgoing edge.");
    return path.getInnerEdges().get(pos);
  }

  /**
   * Get the prefix of the current ARGPath from the first state to the current state (inclusive)
   * returned by this iterator. The prefix will always be forwards directed, thus the {@link
   * ReversePathIterator} does also return the sequence from the first state of the ARGPath up
   * (inclusive) the current position of the iterator.
   *
   * @return A non-null {@link ARGPath}
   */
  public ARGPath getPrefixInclusive() {
    return new ARGPath(
        path.asStatesList().subList(0, pos + 1), path.getInnerEdges().subList(0, pos));
  }

  /**
   * Get the prefix of the current ARGPath from the first state to the current state (exclusive)
   * returned by this iterator. The prefix will always be forwards directed, thus the {@link
   * ReversePathIterator} does also return the sequence from the first state of the ARGPath up
   * (exclusive) the current position of the iterator.
   *
   * <p>May not be called when this iterator points to the first state in the path (at the beginning
   * of an iteration with a forwards PathIterator, or at the end of an iteration with a backwards
   * PathIterator).
   *
   * @return A non-null {@link ARGPath}
   */
  public ARGPath getPrefixExclusive() {
    checkState(pos > 0, "Exclusive prefix of first state in path would be empty.");

    if (pos == 1) {
      return new ARGPath(path.asStatesList().subList(0, pos), ImmutableList.of());
    } else {
      return new ARGPath(
          path.asStatesList().subList(0, pos), path.getInnerEdges().subList(0, pos - 1));
    }
  }

  /**
   * Get the suffix of the current ARGPath from the current state (inclusive) to the last state
   * returned by this iterator. The suffix will always be forwards directed, thus the {@link
   * ReversePathIterator} does also return the sequence from the current state of the ARGPath
   * (inclusive) up to the last position of the iterator.
   *
   * @return A non-null {@link ARGPath}
   */
  public ARGPath getSuffixInclusive() {
    int lastPos = path.size();
    return new ARGPath(
        path.asStatesList().subList(pos, lastPos), path.getInnerEdges().subList(pos, lastPos - 1));
  }

  /**
   * Get the suffix of the current ARGPath from the current state (exclusive) to the last state
   * returned by this iterator. The suffix will always be forwards directed, thus the {@link
   * ReversePathIterator} does also return the sequence from the current state of the ARGPath
   * (exclusive) up to the last position of the iterator.
   *
   * <p>May not be called when this iterator points to the last state in the path (at the end of an
   * iteration with a forwards PathIterator, or at the beginning of an iteration with a backwards
   * PathIterator).
   *
   * @return A non-null {@link ARGPath}
   */
  public ARGPath getSuffixExclusive() {
    checkState(pos < path.size() - 1, "Exclusive suffix of last state in path would be empty.");
    int lastPos = path.size();
    return new ARGPath(
        path.asStatesList().subList(pos + 1, lastPos),
        path.getInnerEdges().subList(pos + 1, lastPos - 1));
  }
}
