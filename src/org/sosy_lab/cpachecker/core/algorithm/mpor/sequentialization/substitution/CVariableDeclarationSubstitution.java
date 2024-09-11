// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.substitution;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.Substitution;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.SubstitutionException;

public class CVariableDeclarationSubstitution implements Substitution {

  private final CVariableDeclaration original;

  private final CVariableDeclaration substitute;

  public CVariableDeclarationSubstitution(
      CVariableDeclaration pOriginal, CVariableDeclaration pSubstitute) {
    original = pOriginal;
    substitute = pSubstitute;
  }

  @Override
  public CExpression substitute(CExpression pExpression) throws SubstitutionException {
    if (pExpression instanceof CIdExpression cIdExpr) {
      if (cIdExpr.getDeclaration().equals(original)) {
        return new CIdExpression(
            cIdExpr.getFileLocation(), cIdExpr.getExpressionType(), cIdExpr.getName(), substitute);
      }
    }
    return pExpression;
  }

  /**
   * Creates a clone of the given CVariableDeclaration with substituted name(s).
   *
   * @param pCVarDec the variable declaration to substitute
   */
  public static CVariableDeclaration createSubstitute(
      CVariableDeclaration pCVarDec, String pNewName) {
    return new CVariableDeclaration(
        pCVarDec.getFileLocation(),
        pCVarDec.isGlobal(),
        pCVarDec.getCStorageClass(),
        pCVarDec.getType(),
        pNewName,
        pNewName,
        pNewName, // TODO funcName::name? not relevant for the sequentialization
        pCVarDec.getInitializer());
  }
}
