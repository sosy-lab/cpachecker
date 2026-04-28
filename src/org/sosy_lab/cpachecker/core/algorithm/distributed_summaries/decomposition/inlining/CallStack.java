// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining.FunctionSCCGraph.FunctionSCC;

public class CallStack {

  private final Optional<CallFrame> topFrame;

  private record CallFrame(Optional<CallFrame> parent, FunctionSCC scc, BlockNode callBlock) {
    void appendAsString(StringBuilder builder) {
      builder.append(scc);
      builder.append(':');
      builder.append(callBlock.getId());
      if (parent.isPresent()) {
        builder.append('@');
        parent.orElseThrow().appendAsString(builder);
      }
    }
  }

  private CallStack(CallFrame topFrame) {
    this.topFrame = Optional.of(topFrame);
  }

  private CallStack(Optional<CallFrame> topFrame) {
    this.topFrame = topFrame;
  }

  private CallStack() {
    topFrame = Optional.empty();
  }

  private static final CallStack EMPTY = new CallStack();

  /** Returns a new CallStack that has the given call site added at the top */
  public CallStack addCall(FunctionSCC scc, BlockNode block) {
    return new CallStack(new CallFrame(topFrame, scc, block));
  }

  public CallStack pop() {
    if (topFrame.isEmpty()) {
      return EMPTY;
    }
    return new CallStack(topFrame.orElseThrow().parent);
  }

  public static CallStack empty() {
    return EMPTY;
  }

  public String asString() {
    if (topFrame.isPresent()) {
      StringBuilder builder = new StringBuilder();
      topFrame.orElseThrow().appendAsString(builder);
      return builder.toString();
    }
    return "";
  }

  @Override
  public String toString() {
    return asString();
  }

  public String toStringWithBlockId(String nodeId) {
    if (topFrame.isEmpty()) {
      return nodeId;
    }

    StringBuilder builder = new StringBuilder();
    builder.append(nodeId);
    builder.append('@');
    topFrame.orElseThrow().appendAsString(builder);
    return builder.toString();
  }

  public FunctionSCC getLastCallScc() {
    return topFrame.map(f -> f.scc()).orElse(null);
  }

  public BlockNode getLastCallBlock() {
    return topFrame.map(f -> f.callBlock()).orElse(null);
  }
}
