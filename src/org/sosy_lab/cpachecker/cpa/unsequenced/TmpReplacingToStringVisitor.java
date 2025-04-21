// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class TmpReplacingToStringVisitor extends DefaultCExpressionVisitor<String, UnrecognizedCodeException> {
  private final Map<String, String> tmpToFunctionMap;

  public TmpReplacingToStringVisitor(Map<String, String> pTmpToFunctionMap) {
    tmpToFunctionMap = pTmpToFunctionMap;
  }

  @Override
  protected String visitDefault(CExpression expr) {
    return expr.toASTString();
  }

  @Override
  public String visit(CIdExpression idExpr) {
    String name = idExpr.getName();
    if (tmpToFunctionMap.containsKey(name)) {
      return tmpToFunctionMap.get(name) + "()";
    }
    return name;
  }

  @Override
  public String visit(CBinaryExpression binExpr) throws UnrecognizedCodeException {
    String left = binExpr.getOperand1().accept(this);
    String right = binExpr.getOperand2().accept(this);
    return left + " " + binExpr.getOperator().getOperator() + " " + right;
  }

  @Override
  public String visit(CUnaryExpression unaryExpr) throws UnrecognizedCodeException {
    return unaryExpr.getOperator().getOperator() + unaryExpr.getOperand().accept(this);
  }

  @Override
  public String visit(CCastExpression castExpr) throws UnrecognizedCodeException {
    return castExpr.getOperand().accept(this);
  }

}
