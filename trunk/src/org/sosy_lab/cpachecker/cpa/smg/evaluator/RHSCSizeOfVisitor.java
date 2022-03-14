// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.math.BigInteger;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelationKind;

class RHSCSizeOfVisitor extends CSizeOfVisitor {

  private final SMGTransferRelationKind kind;

  public RHSCSizeOfVisitor(
      SMGRightHandSideEvaluator pSmgRightHandSideEvaluator,
      CFAEdge pEdge,
      SMGState pState,
      Optional<CExpression> pExpression,
      SMGTransferRelationKind pKind) {
    super(pSmgRightHandSideEvaluator, pEdge, pState, pExpression);
    kind = pKind;
  }

  @Override
  protected BigInteger handleUnkownArrayLengthValue(CArrayType pArrayType) {
    if (kind == SMGTransferRelationKind.REFINEMENT) {
      return BigInteger.ZERO;
    } else {
      return super.handleUnkownArrayLengthValue(pArrayType);
    }
  }
}
