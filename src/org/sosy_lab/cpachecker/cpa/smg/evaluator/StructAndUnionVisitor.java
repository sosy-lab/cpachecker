/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
 * This class evaluates expressions that evaluate to a
 * struct or union type. The type of every expression visited by this
 * visitor has to be either {@link CElaboratedType} or
 * {@link CComplexType}. Furthermore, it must not be a enum.
 * The result of the evaluation is an {@link SMGAddress}.
 * The object represents the memory this struct is placed in, the offset
 * represents the start of the struct.
 */
class StructAndUnionVisitor extends AddressVisitor
    implements CRightHandSideVisitor<List<SMGAddressAndState>, CPATransferException> {

  public StructAndUnionVisitor(SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pCfaEdge, SMGState pNewState) {
    super(pSmgExpressionEvaluator, pCfaEdge, pNewState);
  }

  @Override
  public List<SMGAddressAndState> visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
    return visitDefault(pIastFunctionCallExpression);
  }

  @Override
  public List<SMGAddressAndState> visit(CCastExpression cast) throws CPATransferException {
    CExpression op = cast.getOperand();
    if (smgExpressionEvaluator.isStructOrUnionType(op.getExpressionType())) {
      return cast.getOperand().accept(this);
    } else {
      //TODO cast reinterpretation
      return visitDefault(cast);
    }
  }
}