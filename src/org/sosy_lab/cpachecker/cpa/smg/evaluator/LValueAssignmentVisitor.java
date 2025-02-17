// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * This visitor evaluates the address of a LValue. It is predominantly used to evaluate the left
 * hand side of a Assignment.
 */
public class LValueAssignmentVisitor extends AddressVisitor {

  public LValueAssignmentVisitor(
      SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
    super(pSmgExpressionEvaluator, pEdge, pSmgState);
  }

  @Override
  public List<SMGAddressAndState> visit(CUnaryExpression lValue) throws CPATransferException {
    throw new UnrecognizedCodeException(
        lValue.toASTString() + " is not an lValue", getCfaEdge(), lValue);
  }

  @Override
  public List<SMGAddressAndState> visit(CFunctionCallExpression lValue)
      throws CPATransferException {
    throw new UnrecognizedCodeException(
        lValue.toASTString() + " is not a lValue", getCfaEdge(), lValue);
  }
}
