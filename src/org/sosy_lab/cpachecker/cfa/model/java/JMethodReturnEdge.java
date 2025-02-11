// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.java;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;

public final class JMethodReturnEdge extends FunctionReturnEdge {

  public JMethodReturnEdge(
      FileLocation pFileLocation,
      FunctionExitNode pPredecessor,
      CFANode pSuccessor,
      JMethodSummaryEdge pSummaryEdge) {

    super(pFileLocation, pPredecessor, pSuccessor, pSummaryEdge);
  }

  @Override
  public JMethodSummaryEdge getSummaryEdge() {
    return (JMethodSummaryEdge) super.getSummaryEdge();
  }

  @Override
  public JMethodEntryNode getFunctionEntry() {
    return (JMethodEntryNode) super.getFunctionEntry();
  }

  @Override
  public JMethodOrConstructorInvocation getFunctionCall() {
    return getSummaryEdge().getExpression();
  }
}
