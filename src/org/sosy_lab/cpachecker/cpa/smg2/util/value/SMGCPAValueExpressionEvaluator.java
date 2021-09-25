// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SMGCPAValueExpressionVisitor extends AbstractExpressionValueVisitor {

  protected SMGCPAValueExpressionVisitor(
      String pFunctionName,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger) {
    super(pFunctionName, pMachineModel, pLogger);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
      throws UnrecognizedCodeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Value evaluateCIdExpression(CIdExpression pCIdExpression)
      throws UnrecognizedCodeException {
    // TODO Auto-generated method stub
    return null;
  }



  @Override
  protected Value evaluateCFieldReference(CFieldReference pLValue)
      throws UnrecognizedCodeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
      throws UnrecognizedCodeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Value visit(JClassLiteralExpression pJClassLiteralExpression) throws NoException {
    return throwUnsupportedOperationException("visit(JClassLiteralExpression)");
  }

  @Override
  protected Value evaluateJIdExpression(JIdExpression pVarName) {
    return throwUnsupportedOperationException("evaluateJIdExpression");
  }

  private Value throwUnsupportedOperationException(String methodNameString) {
    throw new AssertionError("The operation " + methodNameString + " is not yet supported.");
  }



}
