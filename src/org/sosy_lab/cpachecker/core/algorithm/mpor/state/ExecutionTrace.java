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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;

public class ExecutionTrace {
  public final ImmutableList<CFAEdge> edges;

  public ExecutionTrace(ImmutableList<CFAEdge> pEdges) {
    edges = pEdges;
  }

  /**
   * Returns the {@link MPORAlgorithm#EXECUTION_TRACE_TAIL_SIZE} last elements of {@link
   * ExecutionTrace#edges}.
   *
   * @return a list containing the {@link MPORAlgorithm#EXECUTION_TRACE_TAIL_SIZE} last elements of
   *     {@link ExecutionTrace#edges}
   */
  public ImmutableList<CFAEdge> tail() {
    int traceSize = edges.size();
    return edges.subList(
        Math.max(traceSize - MPORAlgorithm.EXECUTION_TRACE_TAIL_SIZE, 0), traceSize);
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
