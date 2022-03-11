// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;

public class CFunctionReturnEdge extends FunctionReturnEdge implements CCfaEdge {

  private static final long serialVersionUID = 1988341560860570426L;

  public CFunctionReturnEdge(
      FileLocation pFileLocation,
      FunctionExitNode pPredecessor,
      CFANode pSuccessor,
      CFunctionSummaryEdge pSummaryEdge) {

    super(pFileLocation, pPredecessor, pSuccessor, pSummaryEdge);
  }

  @Override
  public CFunctionSummaryEdge getSummaryEdge() {
    return (CFunctionSummaryEdge) super.getSummaryEdge();
  }

  @Override
  public CFunctionEntryNode getFunctionEntry() {
    return (CFunctionEntryNode) super.getFunctionEntry();
  }

  @Override
  public <R, X extends Exception> R accept(CCfaEdgeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }
}
