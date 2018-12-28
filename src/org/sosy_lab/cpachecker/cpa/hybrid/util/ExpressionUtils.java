/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.hybrid.util;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;

/**
 * This class provides CExpression related functionality
 */
public final class ExpressionUtils {

  // utility class only contains static members
  private ExpressionUtils() {}

  /**
   * Calculate the Expression including the truthAssumption
   * @param pCfaEdge The respective AssumptionEdge of the cfa
   * @param pExpression The already casted expression contained withing the edge
   * @return the (possibly inverted Expression), if the Expression provided by the edge is of type CBinaryExpression,
   *         else an empty Optional
   */
  public static CBinaryExpression getASTWithTruthAssumption(AssumeEdge pCfaEdge, CBinaryExpression pExpression) {

    if(pCfaEdge == null || pCfaEdge.getTruthAssumption()) {

        return pExpression;
    }

    // operator inversion is needed
    return invertExpression(pExpression);
  }

  public static CBinaryExpression invertExpression(CBinaryExpression pExpression) {
    BinaryOperator newOperator = pExpression.getOperator().getOppositLogicalOperator();

    return new CBinaryExpression(
        pExpression.getFileLocation(),
        pExpression.getExpressionType(),
        pExpression.getCalculationType(),
        pExpression.getOperand1(),
        pExpression.getOperand2(),
        newOperator);
  }

  public static boolean checkForVariableIdentifier(CExpression pCExpression) {
    return pCExpression instanceof CIdExpression
        || pCExpression instanceof CArraySubscriptExpression;
  }

  @Nullable
  public static String extractVariableIdentifier(CExpression pExpression) {

    @Nullable
    String identifier = null;
    if (pExpression instanceof CIdExpression) {

      identifier = ((CIdExpression) pExpression).getName();
    } else if (pExpression instanceof CArraySubscriptExpression) {

      CArraySubscriptExpression arraySubscriptExpression = (CArraySubscriptExpression) pExpression;
      CExpression arrayIdentifierExpression = arraySubscriptExpression.getArrayExpression();
      if (arrayIdentifierExpression instanceof CIdExpression) {

        identifier = ((CIdExpression) arrayIdentifierExpression).getName();
      }
    } else if(pExpression instanceof CBinaryExpression) {
      // try to extract for the first operand
      return extractVariableIdentifier(((CBinaryExpression)pExpression).getOperand1());
    }

    return identifier;
  }

  public static boolean isVerifierNondet(CFunctionCallExpression pFunctionCallExpression) {

    final String verifierName = pFunctionCallExpression.getFunctionNameExpression().toASTString();

    return verifierName.startsWith("__VERIFIER_nondet");
  }
} 