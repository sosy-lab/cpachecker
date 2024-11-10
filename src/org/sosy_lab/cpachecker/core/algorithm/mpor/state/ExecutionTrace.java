// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.state;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class ExecutionTrace {

  /**
   * The number of {@link CFAEdge}s to be considered at the end of two {@link ExecutionTrace}s to be
   * approximated as equivalent.
   */
  public static final int TAIL_SIZE = 1;

  public final ImmutableList<CFAEdge> edges;

  public ExecutionTrace(ImmutableList<CFAEdge> pEdges) {
    edges = pEdges;
  }

  /**
   * Returns the {@link ExecutionTrace#TAIL_SIZE} last elements of {@link ExecutionTrace#edges}.
   *
   * @return a list containing the {@link ExecutionTrace#TAIL_SIZE} last elements of {@link
   *     ExecutionTrace#edges}
   */
  public ImmutableList<CFAEdge> tail() {
    int traceSize = edges.size();
    return edges.subList(Math.max(traceSize - TAIL_SIZE, 0), traceSize);
  }

  /**
   * Creates and returns a new ExecutionTrace concatenating { {@link ExecutionTrace#edges}, pEdge }
   *
   * @param pEdge the CFAEdge at the end of the returned ExecutionTrace
   * @return the newly created ExecutionTrace
   */
  public ExecutionTrace add(CFAEdge pEdge) {
    ImmutableList.Builder<CFAEdge> traceBuilder = ImmutableList.builder();
    traceBuilder.addAll(edges);
    traceBuilder.add(pEdge);
    return new ExecutionTrace(traceBuilder.build());
  }
}
