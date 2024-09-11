// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.substitution;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.Substitution;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.SubstitutionException;

public class CVariableDeclarationSubstitution implements Substitution {

  private final CVariableDeclaration original;

  private final CVariableDeclaration substitute;

  private final CBinaryExpressionBuilder binExprBuilder;

  public CVariableDeclarationSubstitution(
      CVariableDeclaration pOriginal,
      CVariableDeclaration pSubstitute,
      CBinaryExpressionBuilder pCBinExprBuilder) {
    original = pOriginal;
    substitute = pSubstitute;
    binExprBuilder = pCBinExprBuilder;
  }

  @Override
  public CExpression substitute(CExpression pExpression) throws SubstitutionException {

    if (pExpression instanceof CIdExpression cIdExpr) {
      if (cIdExpr.getDeclaration().equals(original)) {
        return new CIdExpression(
            cIdExpr.getFileLocation(),
            cIdExpr.getExpressionType(),
            substitute.getName(),
            substitute);
      }

      // recursively substitute operands of binary expressions
    } else if (pExpression instanceof CBinaryExpression cBinExpr) {
      CExpression leftSubstitute = substituteOperand(cBinExpr.getOperand1());
      CExpression rightSubstitute = substituteOperand(cBinExpr.getOperand2());
      // only create a new expression if any operand was substituted
      if (!leftSubstitute.equals(cBinExpr.getOperand1())
          || !rightSubstitute.equals(cBinExpr.getOperand2())) {
        try {
          return binExprBuilder.buildBinaryExpression(
              leftSubstitute, rightSubstitute, cBinExpr.getOperator());
          // "convert" exceptions
        } catch (UnrecognizedCodeException e) {
          throw new SubstitutionException(e.getMessage());
        }
      }
    }
    return pExpression;
  }

  private CExpression substituteOperand(CExpression pOperand) throws SubstitutionException {
    if (pOperand instanceof CIdExpression cIdExpr) {
      return substitute(cIdExpr);
    } else if (pOperand instanceof CBinaryExpression cBinExpr) {
      return substitute(cBinExpr);
    }
    return pOperand;
  }
}
