// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqExpressionBuilder {

  // CIntegerLiteralExpression =====================================================================

  public static CIntegerLiteralExpression buildIntegerLiteralExpression(int pValue) {
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(pValue));
  }

  // CIdExpression =================================================================================

  /**
   * Returns a {@link CIdExpression} with a declaration of the form {@code int {pVariableName} =
   * {pInitializer};}.
   */
  public static CIdExpression buildIdExpressionWithIntegerInitializer(
      boolean pIsGlobal, CSimpleType pType, String pVariableName, CInitializer pInitializer) {

    CVariableDeclaration variableDeclaration =
        SeqDeclarationBuilder.buildVariableDeclaration(
            pIsGlobal, pType, pVariableName, pInitializer);
    return new CIdExpression(FileLocation.DUMMY, variableDeclaration);
  }

  public static CIdExpression buildIdExpression(CSimpleDeclaration pDeclaration) {
    return new CIdExpression(FileLocation.DUMMY, pDeclaration);
  }

  // Helper ========================================================================================

  public static CExpression nestBinaryExpressions(
      ImmutableCollection<CExpression> pAllExpressions,
      BinaryOperator pBinaryOperator,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pAllExpressions.isEmpty(), "pAllExpressions must not be empty");

    CExpression rNested = pAllExpressions.iterator().next();
    for (CExpression next : pAllExpressions) {
      if (!next.equals(rNested)) {
        rNested = pBinaryExpressionBuilder.buildBinaryExpression(rNested, next, pBinaryOperator);
      }
    }
    return rNested;
  }
}
