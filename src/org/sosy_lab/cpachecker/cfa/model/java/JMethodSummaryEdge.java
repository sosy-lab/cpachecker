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
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public class JMethodSummaryEdge extends FunctionSummaryEdge {


  private static final long serialVersionUID = -8173820285051148491L;

  public JMethodSummaryEdge(String pRawStatement, FileLocation pFileLocation,
      CFANode pPredecessor, CFANode pSuccessor,
      JMethodOrConstructorInvocation pExpression, JMethodEntryNode pMethodEntry) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pExpression,
        pMethodEntry);
  }

  @Override
  public JMethodOrConstructorInvocation getExpression() {
    return (JMethodOrConstructorInvocation)super.getExpression();
  }

  @Override
  public JMethodEntryNode getFunctionEntry() {
    return (JMethodEntryNode)super.getFunctionEntry();
  }
}