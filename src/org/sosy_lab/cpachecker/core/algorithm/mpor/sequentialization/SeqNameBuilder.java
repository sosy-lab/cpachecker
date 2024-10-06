// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqPrefix;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SeqNameBuilder {

  private static int varId = 0;

  private static String createVarId() {
    return SeqSyntax.UNDERSCORE + varId++ + SeqSyntax.UNDERSCORE;
  }

  public static String createFuncName(String pFuncName) {
    return SeqPrefix.FUNCTION + SeqSyntax.UNDERSCORE + pFuncName;
  }

  /**
   * Returns a var name of the form {@code __g_{varId}_{pVarDec.getName()}} for global variables and
   * {@code __t{threadId}_{varId}_{varName}} for thread local variables.
   */
  public static String createVarName(CVariableDeclaration pVarDec, int pThreadId) {
    String prefix =
        pVarDec.isGlobal() ? SeqPrefix.VAR_SUB_GLOBAL : SeqPrefix.VAR_SUB_THREAD + pThreadId;
    return prefix + createVarId() + pVarDec.getName();
  }

  /** Returns a var name of the form {@code __p{pThreadId}_{varId}_{pParamDec.getName()}}. */
  public static String createParamName(CParameterDeclaration pParamDec, int pThreadId) {
    return SeqPrefix.VAR_SUB_PARAMETER + pThreadId + createVarId() + pParamDec.getName();
  }

  /** Returns a var name of the form {@code __return_pc_t{pThreadId}_{pFuncName}}. */
  public static String createReturnPcName(int pThreadId, String pFuncName) {
    return SeqPrefix.RETURN_PC_THREAD + pThreadId + SeqSyntax.UNDERSCORE + pFuncName;
  }

  /** Returns a var name of the form {@code __t{pThreadId}_active} */
  public static String createThreadActiveName(int pThreadId) {
    return SeqPrefix.THREAD_ACTIVE + pThreadId + SeqSyntax.UNDERSCORE + SeqToken.ACTIVE;
  }

  /** Returns a var name of the form {@code {pMutexName}_locked} */
  public static String createMutexLockedName(String pMutexName) {
    return pMutexName + SeqSyntax.UNDERSCORE + SeqToken.LOCKED;
  }

  /** Returns a var name of the form {@code __t{pThreadId}_awaits_{pMutexName}} */
  public static String createMutexLockedName(int pThreadId, String pMutexName) {
    return SeqPrefix.THREAD_AWAITS
        + pThreadId
        + SeqSyntax.UNDERSCORE
        + SeqToken.AWAITS
        + SeqSyntax.UNDERSCORE
        + pMutexName;
  }

  /** Returns a var name of the form {@code __t{pWaitingId}_joins_t{pTargetId}} */
  public static String createThreadJoinsName(int pWaitingId, int pTargetId) {
    return SeqPrefix.THREAD_JOINING
        + pWaitingId
        + SeqSyntax.UNDERSCORE
        + SeqToken.JOINS
        + SeqSyntax.UNDERSCORE
        + SeqToken.T
        + pTargetId;
  }

  public static String createQualifiedName(String pVarName) {
    // TODO the qualified names are not relevant in the seq, so we just use dummy::
    return SeqToken.DUMMY + SeqSyntax.COLON + SeqSyntax.COLON + pVarName;
  }

  // TODO unused
  /**
   * Returns the name of {@code pVarDec} with the amount of pointers in the declaration. E.g. {@code
   * int *i;} -> return {@code *i}.
   */
  public static String createParamPointerName(CVariableDeclaration pVarDec) {
    StringBuilder rName = new StringBuilder();
    CType type = pVarDec.getType();
    while (type instanceof CPointerType pointerType) {
      type = pointerType.getType();
      rName.append(SeqSyntax.POINTER);
    }
    rName.append(pVarDec.getName());
    return rName.toString();
  }
}
