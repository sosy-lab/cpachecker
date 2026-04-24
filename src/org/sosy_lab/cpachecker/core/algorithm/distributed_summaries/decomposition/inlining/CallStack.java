// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining.FunctionSCCGraph.FunctionSCC;

// TODO is this too inefficient? Using a linked List like structure would save a lot of copies
public record CallStack(ImmutableList<CallSite> calls) {

  private static final CallStack EMPTY = new CallStack(ImmutableList.of());

  private record CallSite(FunctionSCC scc, BlockNode block) {
    @Override
    public String toString() {
      return scc + ":" + block.getId();
    }
  }

  /** Returns a new CallStack that has the given call site added at the top */
  public CallStack addCall(FunctionSCC scc, BlockNode block) {

    ImmutableList.Builder<CallSite> builder = ImmutableList.builder();
    builder.addAll(calls);
    builder.add(new CallSite(scc, block));

    return new CallStack(builder.build());
  }

  public CallStack pop() {
    if (calls.isEmpty()) {
      return this;
    }
    ImmutableList.Builder<CallSite> builder = ImmutableList.builder();
    // copy all except the last (top) element
    for (int i = 0; i < calls.size() - 1; i++) {
      builder.add(calls.get(i));
    }
    ImmutableList<CallSite> newCalls = builder.build();
    return newCalls.isEmpty() ? EMPTY : new CallStack(newCalls);
  }

  public static CallStack empty() {
    return EMPTY;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    // We want to read it in reverse order
    for (int i = calls.size() - 1; i >= 0; i--) {
      builder.append(calls.get(i));
      if (i != 0) {
        builder.append('@');
      }
    }

    return builder.toString();
  }

  public String toStringWithBlockId(String nodeId) {
    if (equals(empty())) {
      return nodeId;
    } else {
      return nodeId + "@" + this;
    }
  }

  public BlockNode getLastCallBlock() {
    if (calls.isEmpty()) {
      return null;
    }
    return calls.getLast().block;
  }

  public FunctionSCC getLastCallScc() {
    if (calls.isEmpty()) {
      return null;
    }
    return calls.getLast().scc;
  }
}
