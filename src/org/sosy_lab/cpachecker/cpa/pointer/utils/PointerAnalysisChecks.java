// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.utils;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class PointerAnalysisChecks {

  private PointerAnalysisChecks() {}

  public static boolean isFreeFunction(CExpression pExpression) {
    return getFunctionName(pExpression).map("free"::equals).orElse(false);
  }

  public static boolean isMallocFunction(CExpression pExpression) {
    return getFunctionName(pExpression).map("malloc"::equals).orElse(false);
  }

  public static boolean isNondetPointerReturn(CExpression pExpression) {
    return getFunctionName(pExpression).map("__VERIFIER_nondet_pointer"::equals).orElse(false);
  }

  private static Optional<String> getFunctionName(CExpression pExpression) {
    if (pExpression instanceof CIdExpression idExpr) {
      return Optional.of(idExpr.getName());
    }
    return Optional.empty();
  }

  public static boolean isNullPointer(CExpression pExpression) {
    if (pExpression instanceof CCastExpression castExpression) {
      CExpression operand = castExpression.getOperand();
      if (operand instanceof CIntegerLiteralExpression intLiteral
          && intLiteral.getValue().longValue() == 0) {
        return true;
      }
    }
    return pExpression instanceof CIntegerLiteralExpression intLiteral
        && intLiteral.getValue().longValue() == 0;
  }

  public static boolean isNullPointer(CRightHandSide pRhs) {
    return (pRhs instanceof CExpression cExpr) && isNullPointer(cExpr);
  }

  public static Optional<MemoryLocation> getFunctionReturnVariable(
      FunctionEntryNode pFunctionEntryNode) {
    Optional<? extends AVariableDeclaration> returnVariable =
        pFunctionEntryNode.getReturnVariable();
    return returnVariable.map(MemoryLocation::forDeclaration);
  }
}
