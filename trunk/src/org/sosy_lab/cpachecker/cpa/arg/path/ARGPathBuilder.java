// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.path;

import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

/**
 * A class for creating {@link ARGPath}s by iteratively adding path elements one after another.
 * ARGPaths can be built either from the beginning to the endpoint or in reverse. The builder can
 * still be used after calling {@link #build(ARGState)}. Please note that the state and edge given
 * to the build method will not be added permanently to the builder. If they should be in the
 * builder afterwards you need to use {@link #add(ARGState, CFAEdge)}.
 *
 * <p>In the future we want to remove the edge given to the build method. An outgoing edge of the
 * last state of a path does not make sense.
 */
public abstract class ARGPathBuilder {

  final List<ARGState> states = new ArrayList<>();
  final List<CFAEdge> edges = new ArrayList<>();

  private ARGPathBuilder() {}

  /** Returns the amount of states which are currently added to this builder. */
  public int size() {
    return states.size();
  }

  /** Add the given state and edge to the ARGPath that should be created. */
  @CanIgnoreReturnValue
  public ARGPathBuilder add(ARGState state, CFAEdge outgoingEdge) {
    states.add(state);
    edges.add(outgoingEdge);
    return this;
  }

  /** Remove the state and edge that were added at last. */
  @CanIgnoreReturnValue
  public ARGPathBuilder removeLast() {
    assert !states.isEmpty() && !edges.isEmpty();
    states.remove(states.size() - 1);
    edges.remove(edges.size() - 1);
    return this;
  }

  /** Build the ARGPath using the given state as the last state. */
  public abstract ARGPath build(ARGState state);

  /**
   * The implementation of the ARGPathBuilder that adds new states and edges at the end of the Path.
   */
  static class DefaultARGPathBuilder extends ARGPathBuilder {

    @Override
    public ARGPath build(ARGState pState) {
      states.add(pState);
      ARGPath path = new ARGPath(states, edges);
      states.remove(states.size() - 1);
      return path;
    }
  }

  /**
   * The implementation of the ARGPathBuilder that adds new states and edges at the beginning of the
   * Path.
   */
  static class ReverseARGPathBuilder extends ARGPathBuilder {

    @Override
    public ARGPath build(ARGState pState) {
      states.add(pState);
      ARGPath path = new ARGPath(Lists.reverse(states), Lists.reverse(edges));
      states.remove(states.size() - 1);
      return path;
    }
  }
}
