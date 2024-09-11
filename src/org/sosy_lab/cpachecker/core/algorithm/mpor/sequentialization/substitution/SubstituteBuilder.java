// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.substitution;

import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SubstituteBuilder {

  private static int varId = 0;

  private static String createVarId() {
    return SeqSyntax.UNDERSCORE + varId++ + SeqSyntax.UNDERSCORE;
  }

  public static String createGlobalVarSubstituteName(CVariableDeclaration pCVarDec) {
    return SeqToken.PREFIX_GLOBAL + createVarId() + pCVarDec.getName();
  }

  public static String createLocalVarSubstituteName(CVariableDeclaration pCVarDec, int pThreadId) {
    return SeqToken.PREFIX_THREAD + pThreadId + createVarId() + pCVarDec.getName();
  }

  // TODO createParameterVarSubstituteName

  /**
   * Creates a clone of the given CVariableDeclaration with substituted name(s).
   *
   * @param pCVarDec the variable declaration to substitute
   */
  public static CVariableDeclaration createVarSubstitute(
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
