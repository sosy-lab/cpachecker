// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelationKind;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

class RHSPointerAddressVisitor extends PointerVisitor {

  private final SMGRightHandSideEvaluator smgRightHandSideEvaluator;
  private final SMGTransferRelationKind kind;

  public RHSPointerAddressVisitor(
      SMGRightHandSideEvaluator pSmgRightHandSideEvaluator,
      CFAEdge pEdge,
      SMGState pSmgState,
      SMGTransferRelationKind pKind) {
    super(pSmgRightHandSideEvaluator, pEdge, pSmgState);
    smgRightHandSideEvaluator = pSmgRightHandSideEvaluator;
    kind = pKind;
  }

  @Override
  protected List<SMGAddressValueAndState> createAddressOfFunction(
      CIdExpression pIdFunctionExpression) throws SMGInconsistentException {
    SMGState state = getInitialSmgState();
    CFunctionDeclaration functionDcl = (CFunctionDeclaration) pIdFunctionExpression.getDeclaration();
    SMGObject functionObject = state.getObjectForFunction(functionDcl);

    if (functionObject == null) {
      functionObject = state.createObjectForFunction(functionDcl);
    }

    return smgRightHandSideEvaluator.createAddress(
        state, functionObject, SMGZeroValue.INSTANCE);
  }

  @Override
  public List<? extends SMGValueAndState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
    String functionName = fileNameExpression.toASTString();
    return smgRightHandSideEvaluator.builtins.handleFunctioncall(
        pIastFunctionCallExpression, functionName, getInitialSmgState(), getCfaEdge(), kind);
  }
}