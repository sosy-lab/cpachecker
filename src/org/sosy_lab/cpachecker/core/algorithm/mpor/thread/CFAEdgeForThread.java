// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class CFAEdgeForThread {

  private static int currentId = 0;

  private static int getNewId() {
    return currentId++;
  }

  public static void resetId() {
    currentId = 0;
  }

  public final int id;

  public final int threadId;

  public final CFAEdge cfaEdge;

  /** Not all edges have a calling context, e.g. {@code main()} function statements. */
  public final Optional<CFAEdgeForThread> callContext;

  @Nullable private CFANodeForThread predecessor = null;

  @Nullable private CFANodeForThread successor = null;

  public CFAEdgeForThread(
      int pThreadId, CFAEdge pCfaEdge, Optional<CFAEdgeForThread> pCallContext) {

    id = getNewId();
    threadId = pThreadId;
    cfaEdge = pCfaEdge;
    callContext = pCallContext;
  }

  public CFANodeForThread getPredecessor() {
    checkArgument(predecessor != null, "predecessor not set yet");
    return predecessor;
  }

  protected void setPredecessor(CFANodeForThread pPredecessor) {
    checkNotNull(pPredecessor);
    checkArgument(predecessor == null, "predecessor set already");
    predecessor = pPredecessor;
  }

  public CFANodeForThread getSuccessor() {
    checkArgument(successor != null, "successor not set yet");
    return successor;
  }

  protected void setSuccessor(CFANodeForThread pSuccessor) {
    checkNotNull(pSuccessor);
    checkArgument(successor == null, "successor set already");
    successor = pSuccessor;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        threadId,
        cfaEdge,
        predecessor == null ? null : predecessor.id,
        successor == null ? null : successor.id);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof CFAEdgeForThread other
        && id == other.id
        && threadId == other.threadId
        && cfaEdge.equals(other.cfaEdge)
        && (predecessor == null || predecessor.id == other.predecessor.id)
        && (successor == null || successor.id == other.successor.id);
  }
}
