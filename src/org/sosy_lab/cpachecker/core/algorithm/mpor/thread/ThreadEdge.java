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

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;

public class ThreadEdge {

  public final CFAEdge cfaEdge;

  /** Not all edges have a calling context, e.g. {@code main()} function statements. */
  public final Optional<CFunctionCallEdge> callingContext;

  private ThreadNode predecessor = null;

  private ThreadNode successor = null;

  public ThreadEdge(CFAEdge pCfaEdge, Optional<CFunctionCallEdge> pCallingContext) {
    cfaEdge = pCfaEdge;
    callingContext = pCallingContext;
  }

  public ThreadNode getPredecessor() {
    checkArgument(predecessor != null, "predecessor not set yet");
    return predecessor;
  }

  protected void setPredecessor(ThreadNode pPredecessor) {
    checkNotNull(pPredecessor);
    checkArgument(predecessor == null, "predecessor set already");
    predecessor = pPredecessor;
  }

  public ThreadNode getSuccessor() {
    checkArgument(successor != null, "successor not set yet");
    return successor;
  }

  protected void setSuccessor(ThreadNode pSuccessor) {
    checkNotNull(pSuccessor);
    checkArgument(successor == null, "successor set already");
    successor = pSuccessor;
  }
}
