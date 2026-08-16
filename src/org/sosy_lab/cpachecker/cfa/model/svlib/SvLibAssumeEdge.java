// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public final class SvLibAssumeEdge extends AssumeEdge implements SvLibCfaEdge {

  public SvLibAssumeEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      SvLibRelationalTerm pExpression,
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
  public SvLibRelationalTerm getExpression() {
    return (SvLibRelationalTerm) super.getExpression();
  }
}
