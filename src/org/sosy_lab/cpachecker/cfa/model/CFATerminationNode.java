// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import com.google.errorprone.annotations.DoNotCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;

/** A CFANode that marks the end of a path. */
public final class CFATerminationNode extends CFANode {

  private static final long serialVersionUID = -8328879108494506389L;

  public CFATerminationNode(AFunctionDeclaration pFunction) {
    super(pFunction);
  }

  @Override
  public void addLeavingEdge(CFAEdge pNewLeavingEdge) {
    throw new AssertionError(pNewLeavingEdge);
  }

  @Override
  public void addLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    throw new AssertionError(pEdge);
  }

  @Override
  @Deprecated
  @DoNotCall // safe to call but useless
  public int getNumLeavingEdges() {
    return 0;
  }

  @Override
  @Deprecated
  @DoNotCall
  public CFAEdge getLeavingEdge(int pIndex) {
    throw new IndexOutOfBoundsException();
  }

  @Override
  @Deprecated
  @DoNotCall // safe to call but useless
  public FunctionSummaryEdge getLeavingSummaryEdge() {
    return null;
  }
}
