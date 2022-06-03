// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public class CFunctionSummaryEdge extends FunctionSummaryEdge implements CCfaEdge {

  private static final long serialVersionUID = -2005621000523551217L;

  public CFunctionSummaryEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      CFunctionCall pExpression,
      CFunctionEntryNode pFunctionEntry) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pExpression, pFunctionEntry);
  }

  @Override
  public CFunctionCall getExpression() {
    return (CFunctionCall) super.getExpression();
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
