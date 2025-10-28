// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.k3;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3RelationalTerm;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public final class K3AssumeEdge extends AssumeEdge implements K3CfaEdge {

  public K3AssumeEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      K3RelationalTerm pExpression,
      boolean pTruthAssumption,
      boolean pSwapped,
      boolean pArtificialIntermediate) {
    super(
        pRawStatement,
        pFileLocation,
        pPredecessor,
        pSuccessor,
        pExpression,
        pTruthAssumption,
        pSwapped,
        pArtificialIntermediate);
  }

  @Override
  public K3RelationalTerm getExpression() {
    return (K3RelationalTerm) super.getExpression();
  }
}
