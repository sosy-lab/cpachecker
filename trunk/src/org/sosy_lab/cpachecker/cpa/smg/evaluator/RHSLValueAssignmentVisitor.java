// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

class RHSLValueAssignmentVisitor extends LValueAssignmentVisitor {

  public RHSLValueAssignmentVisitor(
      SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
    super(pSmgExpressionEvaluator, pEdge, pSmgState);
  }

  @Override
  public List<SMGAddressAndState> visit(CPointerExpression pLValue) throws CPATransferException {
    List<SMGAddressAndState> results = new ArrayList<>();
    for (SMGAddressAndState address : super.visit(pLValue)) {
      if (address.getObject().isUnknown()) {
        address =
            SMGAddressAndState.withUnknownAddress(address.getSmgState().withUnknownDereference());
      }
      results.add(address);
    }
    return results;
  }
}
