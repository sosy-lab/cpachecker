// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.k3;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3CfaEdgeStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class K3StatementEdge extends AStatementEdge implements K3CfaEdge {
  public K3StatementEdge(
      String pRawStatement,
      K3CfaEdgeStatement pStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor) {
    super(pRawStatement, pStatement, pFileLocation, pPredecessor, pSuccessor);
  }
}
