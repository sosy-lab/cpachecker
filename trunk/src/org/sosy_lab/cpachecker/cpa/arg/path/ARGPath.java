// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.path;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.ForOverride;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPathBuilder.DefaultARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPathBuilder.ReverseARGPathBuilder;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * ARGPath contains a non-empty path through the ARG consisting of both a sequence of states and the
 * edges between them. Very often, the first state is the root state of the ARG, and the last state
 * is a target state, though this is not guaranteed.
 *
 * <p>The number of states is always one larger than the number of edges.
 *
 * <p>States on this path cannot be null. Edges can be null, if there is no corresponding CFAEdge
 * between two consecutive abstract states.
 *
 * <p>The recommended way to iterate through an ARGPath if you need both states and edges is to use
 * {@link #pathIterator()}.
 *
 * <p>The usual way to get an ARGPath instance is from methods in {@link ARGUtils} such as {@link
 * ARGUtils#getOnePathTo(ARGState)} and {@link ARGUtils#getRandomPath(ARGState)}.
 */
@javax.annotation.concurrent.Immutable // cannot prove deep immutability
public class ARGPath extends AbstractAppender {

  private final ImmutableList<ARGState> states;
  private final List<CFAEdge> edges; // immutable, but may contain null

  @SuppressFBWarnings(
      value = "JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS",
      justification =
          "This variable is only used for caching the full path for later use"
              + " without having to compute it again.")
  private List<CFAEdge> fullPath = null;

  protected ARGPath(ARGPath pArgPath) {
    states = pArgPath.states;
    edges = pArgPath.edges;
  }

  public ARGPath(List<ARGState> pStates) {
    checkArgument(!pStates.isEmpty(), "ARGPaths may not be empty");
    states = ImmutableList.copyOf(pStates);

    List<CFAEdge> edgesBuilder = new ArrayList<>(states.size() - 1);
    for (int i = 0; i < states.size() - 1; i++) {
      ARGState parent = states.get(i);
      ARGState child = states.get(i + 1);
      edgesBuilder.add(parent.getEdgeToChild(child)); // may return null
    }

    edges = Collections.unmodifiableList(edgesBuilder);
    assert states.size() - 1 == edges.size();
  }

  public ARGPath(List<ARGState> pStates, List<CFAEdge> pEdges) {
    checkArgument(!pStates.isEmpty(), "ARGPaths may not be empty");
    checkArgument(
        pStates.size() - 1 == pEdges.size(), "ARGPaths must have one state more than edges");

    states = ImmutableList.copyOf(pStates);
    edges = Collections.unmodifiableList(new ArrayList<>(pEdges));
  }

  public ImmutableList<ARGState> asStatesList() {
    return states;
  }

  /**
   * Return the list of edges between the states. The result of this method is always one element
   * shorter than {@link #asStatesList()}.
   */
  public List<CFAEdge> getInnerEdges() {
    return edges;
  }

  /**
   * Returns the full path contained in this {@link ARGPath}. This means, edges which are null while
   * using getInnerEdges or the pathIterator will be resolved and the complete path from the first
   * {@link ARGState} to the last ARGState is created. This is done by filling up the wholes in the
   * path.
   *
   * <p>If there is no path (null edges can not be filled up, may be happening when using bam) we
   * return an empty list instead.
   */
  public List<CFAEdge> getFullPath() {
    if (fullPath == null) {
      fullPath = buildFullPath();
    }
    return fullPath;
  }

  /**
   * Compute a full list of CFAedges along the given list of ARGStates.
   *
   * <p>This method is intended to be only called lazily and only once, because it might be
   * expensive.
   */
  @ForOverride
  protected List<CFAEdge> buildFullPath() {
    ImmutableList.Builder<CFAEdge> newFullPath = ImmutableList.builder();
    PathIterator it = pathIterator();

    while (it.hasNext()) {
      ARGState prev = it.getAbstractState();
      CFAEdge curOutgoingEdge = it.getOutgoingEdge();
      it.advance();
      ARGState succ = it.getAbstractState();

      // assert prev.getEdgeToChild(succ) == curOutgoingEdge : "invalid ARGPath";

      // compute path between cur and next node
      if (curOutgoingEdge == null) {
        final List<CFAEdge> intermediateEdges = prev.getEdgesToChild(succ);
        if (intermediateEdges.isEmpty()) {
          return ImmutableList.of();
        }
        newFullPath.addAll(intermediateEdges);

        // we have a normal connection without hole in the edges
      } else {
        newFullPath.add(curOutgoingEdge);
      }
    }

    return newFullPath.build();
  }

  public ImmutableSet<ARGState> getStateSet() {
    return ImmutableSet.copyOf(states);
  }

  /** Return (predecessor,successor) pairs of ARGStates for every edge in the path. */
  public List<Pair<ARGState, ARGState>> getStatePairs() {
    return new AbstractList<>() {

      @Override
      public Pair<ARGState, ARGState> get(int pIndex) {
        return Pair.of(states.get(pIndex), states.get(pIndex + 1));
      }

      @Override
      public int size() {
        return states.size() - 1;
      }
    };
  }

  /**
   * Create a fresh {@link PathIterator} for this path, with its position at the first state. Note
   * that you cannot call {@link PathIterator#getIncomingEdge()} before calling {@link
   * PathIterator#advance()} at least once.
   */
  public PathIterator pathIterator() {
    return new DefaultPathIterator(this);
  }

  /**
   * Create a fresh {@link PathIterator} for this path, with its position at the last state and
   * iterating backwards. Note that you cannot call {@link PathIterator#getOutgoingEdge()} before
   * calling {@link PathIterator#advance()} at least once.
   */
  public PathIterator reversePathIterator() {
    return new ReversePathIterator(this);
  }

  /**
   * Create a fresh {@link PathIterator} for this path, with its position at the first state. Holes
   * in the path are filled up by inserting more {@link CFAEdge}. Note that you cannot call {@link
   * PathIterator#getIncomingEdge()} before calling {@link PathIterator#advance()} at least once.
   */
  public PathIterator fullPathIterator() {
    return new DefaultFullPathIterator(this);
  }

  /**
   * Create a fresh {@link PathIterator} for this path, with its position at the last state and
   * iterating backwards. Holes in the path are filled up by inserting more {@link CFAEdge}. Note
   * that you cannot call {@link PathIterator#getOutgoingEdge()} before calling {@link
   * PathIterator#advance()} at least once.
   */
  public PathIterator reverseFullPathIterator() {
    return new ReverseFullPathIterator(this);
  }

  /**
   * A forward directed {@link ARGPathBuilder} with no initial states and edges added. (States and
   * edges are always appended to the end of the current path)
   */
  public static ARGPathBuilder builder() {
    return new DefaultARGPathBuilder();
  }

  /**
   * A backward directed {@link ARGPathBuilder} with no initial states and edges added. (States and
   * edges are always appended to the beginning of the current path)
   */
  public static ARGPathBuilder reverseBuilder() {
    return new ReverseARGPathBuilder();
  }

  /**
   * The length of the path, i.e., the number of states (this is different from the number of
   * edges).
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
    return Objects.hash(edges);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (!(pOther instanceof ARGPath)) {
      return false;
    }
    // We do not compare the states because they are different from iteration to iteration!
    return Objects.equals(edges, ((ARGPath) pOther).edges);
  }

  @Override
  public void appendTo(Appendable appendable) throws IOException {
    Joiner.on(System.lineSeparator()).skipNulls().appendTo(appendable, getFullPath());
    appendable.append(System.lineSeparator());
  }
}
