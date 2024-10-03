// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.statements;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqNameBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SeqExpressions {

  public static final CIntegerLiteralExpression INT_ONE =
      new CIntegerLiteralExpression(FileLocation.DUMMY, SeqTypes.INT, BigInteger.ONE);

  public static final CIntegerLiteralExpression INT_ZERO =
      new CIntegerLiteralExpression(FileLocation.DUMMY, SeqTypes.INT, BigInteger.ZERO);

  /** Returns a {@link CIdExpression} with a declaration of the form {@code int {pVarName} = 0;}. */
  public static CIdExpression buildIntVar(String pVarName) {
    CVariableDeclaration varDec =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.AUTO,
            SeqTypes.INT,
            pVarName,
            pVarName,
            SeqNameBuilder.createQualifiedName(SeqToken.MAIN, pVarName),
            SeqInitializers.INT_0);
    return new CIdExpression(FileLocation.DUMMY, varDec);
  }

  public static CIdExpression buildIdExpr(CVariableDeclaration pVarDec) {
    return new CIdExpression(FileLocation.DUMMY, pVarDec);
  }

  public static CExpressionAssignmentStatement buildExprAssignStmt(
      CLeftHandSide pLhs, CExpression pRhs) {
    return new CExpressionAssignmentStatement(FileLocation.DUMMY, pLhs, pRhs);
  }
}
