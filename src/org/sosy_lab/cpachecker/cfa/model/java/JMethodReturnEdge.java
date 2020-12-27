// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.java;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;

public class JMethodReturnEdge extends FunctionReturnEdge {


  private static final long serialVersionUID = -8946598759920862594L;

  public JMethodReturnEdge(FileLocation pFileLocation,
      FunctionExitNode pPredecessor, CFANode pSuccessor,
      JMethodSummaryEdge pSummaryEdge) {

    super(pFileLocation, pPredecessor, pSuccessor, pSummaryEdge);

  }

  @Override
  public JMethodSummaryEdge getSummaryEdge() {
    return (JMethodSummaryEdge)super.getSummaryEdge();
  }

  @Override
  public JMethodEntryNode getFunctionEntry() {
    return (JMethodEntryNode)super.getFunctionEntry();
  }
}
