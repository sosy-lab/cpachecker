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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SeqNameBuilder {

  private static int varId = 0;

  private static String createVarId() {
    return SeqSyntax.UNDERSCORE + varId++ + SeqSyntax.UNDERSCORE;
  }

  /** Returns {@code "__MPOR_SEQ__THREAD{pThreadId}_"}. */
  private static String buildThreadPrefix(int pThreadId) {
    return SeqToken.__MPOR_SEQ__ + SeqToken.THREAD + pThreadId + SeqSyntax.UNDERSCORE;
  }

  public static String createFuncName(String pFuncName) {
    return SeqToken.__MPOR_SEQ__ + pFuncName;
  }

  /**
   * Returns a var name of the form {@code GLOBAL_{varId}_{pVarDec.getName()}} for global variables
   * and {@code LOCAL_THREAD{threadId}_{varId}_{varName}} for thread local variables.
   */
  public static String createVarName(CVariableDeclaration pVarDec, int pThreadId) {
    String prefix =
        pVarDec.isGlobal()
            ? SeqToken.GLOBAL
            : SeqToken.LOCAL + SeqSyntax.UNDERSCORE + SeqToken.THREAD + pThreadId;
    return prefix + createVarId() + pVarDec.getName();
  }

  /**
   * Returns a var name of the form {@code PARAM_THREAD{pThreadId}_{varId}_{pParamDec.getName()}}.
   */
  public static String createParamName(CParameterDeclaration pParamDec, int pThreadId) {
    return SeqToken.PARAM
        + SeqSyntax.UNDERSCORE
        + SeqToken.THREAD
        + pThreadId
        + createVarId()
        + pParamDec.getName();
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__THREAD{pThreadId}_RETURN_PC_{pFuncName}}. */
  public static String createReturnPcName(int pThreadId, String pFuncName) {
    return buildThreadPrefix(pThreadId) + SeqToken.RETURN_PC + SeqSyntax.UNDERSCORE + pFuncName;
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__THREAD{pThreadId}_ACTIVE} */
  public static String buildThreadActiveName(int pThreadId) {
    return buildThreadPrefix(pThreadId) + SeqToken.ACTIVE;
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__{pMutexName}_LOCKED} */
  public static String buildMutexLockedName(String pMutexName) {
    return SeqToken.__MPOR_SEQ__ + pMutexName + SeqSyntax.UNDERSCORE + SeqToken.LOCKED;
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__THREAD{pThreadId}_AWAITS_{pMutexName}} */
  public static String buildThreadLocksMutexName(int pThreadId, String pMutexName) {
    return buildThreadPrefix(pThreadId) + SeqToken.LOCKS + SeqSyntax.UNDERSCORE + pMutexName;
  }

  /**
   * Returns a var name of the form {@code __MPOR_SEQ__THREAD{pWaitingId}_JOINS_TARGET{pTargetId}}
   */
  public static String buildThreadJoinsThreadName(int pWaitingId, int pTargetId) {
    return buildThreadPrefix(pWaitingId)
        + SeqToken.JOINS
        + SeqSyntax.UNDERSCORE
        + SeqToken.THREAD
        + pTargetId;
  }

  public static String buildAtomicInUseName() {
    return SeqToken.__MPOR_SEQ__
        + SeqToken.ATOMIC
        + SeqSyntax.UNDERSCORE
        + SeqToken.IN
        + SeqSyntax.UNDERSCORE
        + SeqToken.USE;
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__THREAD{pThreadId}_BEGINS_ATOMIC} */
  public static String buildThreadBeginsAtomicName(int pThreadId) {
    return buildThreadPrefix(pThreadId) + SeqToken.BEGINS + SeqSyntax.UNDERSCORE + SeqToken.ATOMIC;
  }

  public static String createQualifiedName(String pVarName) {
    // TODO the qualified names are not relevant in the seq, so we just use dummy::
    return SeqToken.dummy + SeqSyntax.COLON + SeqSyntax.COLON + pVarName;
  }
}
