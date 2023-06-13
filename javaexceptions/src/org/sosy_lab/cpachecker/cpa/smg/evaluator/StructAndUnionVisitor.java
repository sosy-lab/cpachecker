// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * This class evaluates expressions that evaluate to a struct or union type. The type of every
 * expression visited by this visitor has to be either {@link CElaboratedType} or {@link
 * CComplexType}. Furthermore, it must not be a enum. The result of the evaluation is an {@link
 * SMGAddress}. The object represents the memory this struct is placed in, the offset represents the
 * start of the struct.
 */
class StructAndUnionVisitor extends AddressVisitor
    implements CRightHandSideVisitor<List<SMGAddressAndState>, CPATransferException> {

  public StructAndUnionVisitor(
      SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pCfaEdge, SMGState pNewState) {
    super(pSmgExpressionEvaluator, pCfaEdge, pNewState);
  }

  @Override
  public List<SMGAddressAndState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    return visitDefault(pIastFunctionCallExpression);
  }

  @Override
  public List<SMGAddressAndState> visit(CCastExpression cast) throws CPATransferException {
    CExpression op = cast.getOperand();
    if (SMGExpressionEvaluator.isStructOrUnionType(op.getExpressionType())) {
      return cast.getOperand().accept(this);
    } else {
      // TODO cast reinterpretation
      return visitDefault(cast);
    }
  }
}
