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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class ThreadEdge {

  public final CFAEdge cfaEdge;

  public CFAEdge substitute;

  // TODO maybe use Optional here
  private ThreadNode predecessor = null;

  private ThreadNode successor = null;

  public ThreadEdge(CFAEdge pCfaEdge) {
    cfaEdge = pCfaEdge;
  }

  public CFAEdge getSubstitute() {
    return substitute;
  }

  public void setSubstitute(CFAEdge pSubstitute) {
    checkNotNull(pSubstitute);
    checkArgument(substitute == null, "substitute set already");
    substitute = pSubstitute;
  }

  protected void setPredecessor(ThreadNode pPredecessor) {
    checkNotNull(pPredecessor);
    checkArgument(predecessor == null, "predecessor set already");
    predecessor = pPredecessor;
  }

  public ThreadNode getPredecessor() {
    return predecessor;
  }

  protected void setSuccessor(ThreadNode pSuccessor) {
    checkNotNull(pSuccessor);
    checkArgument(successor == null, "successor set already");
    successor = pSuccessor;
  }

  public ThreadNode getSuccessor() {
    return successor;
  }
}
