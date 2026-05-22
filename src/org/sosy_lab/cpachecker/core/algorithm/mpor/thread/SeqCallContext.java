// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

/**
 * A record to represent a call context in the input program.
 *
 * @param cfaEdgeForThread The {@link CFAEdgeForThread} that calls a function or start routine. This
 *     is optional because not all edges have a calling context, e.g., all edges in the {@code
 *     main()} function.
 */
public record SeqCallContext(Optional<CFAEdgeForThread> cfaEdgeForThread) {

  public SeqCallContext {
    if (cfaEdgeForThread.isPresent()) {
      CFAEdgeForThread threadEdge = cfaEdgeForThread.orElseThrow();
      checkArgument(
          threadEdge.cfaEdge instanceof CFunctionCallEdge
              || threadEdge.cfaEdge instanceof CFunctionSummaryEdge
              || threadEdge.cfaEdge instanceof CStatementEdge,
          "Call context CFAEdge must be CFunctionCallEdge, CFunctionSummaryEdge or"
              + " CStatementEdge.");
    }
  }

  public boolean isStartRoutineCallContext() {
    return cfaEdgeForThread.isPresent()
        && cfaEdgeForThread.orElseThrow().cfaEdge instanceof CStatementEdge;
  }
}
