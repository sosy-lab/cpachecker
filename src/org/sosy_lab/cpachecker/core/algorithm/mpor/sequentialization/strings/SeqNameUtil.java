// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings;

import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqNameUtil {

  private static int varId = 0;

  private static String createVarId() {
    return SeqSyntax.UNDERSCORE + varId++ + SeqSyntax.UNDERSCORE;
  }

  /** Returns {@code "__MPOR_SEQ__THREAD{pThreadId}_"}. */
  private static String buildThreadPrefix(int pThreadId) {
    return SeqToken.__MPOR_SEQ__ + SeqToken.THREAD + pThreadId + SeqSyntax.UNDERSCORE;
  }

  /** Returns {@code "__MPOR_SEQ__{pFuncName}"}. */
  public static String buildFuncName(String pFuncName) {
    return SeqToken.__MPOR_SEQ__ + pFuncName;
  }

  /**
   * Returns a var name of the form {@code GLOBAL_{varId}_{pVarDec.getName()}} for global variables
   * and {@code LOCAL_THREAD{threadId}_{varId}_{varName}} for thread local variables.
   */
  public static String buildVariableName(CVariableDeclaration pVarDec, int pThreadId) {
    String prefix =
        pVarDec.isGlobal()
            ? SeqToken.GLOBAL
            : SeqToken.LOCAL + SeqSyntax.UNDERSCORE + SeqToken.THREAD + pThreadId;
    return prefix + createVarId() + pVarDec.getName();
  }

  /**
   * Returns a var name of the form {@code PARAM_THREAD{pThreadId}_{varId}_{pParamDec.getName()}}.
   */
  public static String buildParameterName(CParameterDeclaration pParamDec, int pThreadId) {
    return SeqToken.PARAM
        + SeqSyntax.UNDERSCORE
        + SeqToken.THREAD
        + pThreadId
        + createVarId()
        + pParamDec.getName();
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__THREAD{pThreadId}_RETURN_PC_{pFuncName}}. */
  public static String buildReturnPcName(int pThreadId, String pFuncName) {
    return buildThreadPrefix(pThreadId) + SeqToken.RETURN_PC + SeqSyntax.UNDERSCORE + pFuncName;
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

  public static String buildAtomicLockedName() {
    return SeqToken.__MPOR_SEQ__ + SeqToken.ATOMIC + SeqSyntax.UNDERSCORE + SeqToken.LOCKED;
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__THREAD{pThreadId}_BEGINS_ATOMIC} */
  public static String buildThreadBeginsAtomicName(int pThreadId) {
    return buildThreadPrefix(pThreadId) + SeqToken.BEGINS + SeqSyntax.UNDERSCORE + SeqToken.ATOMIC;
  }

  public static String buildQualifiedName(String pVarName) {
    // TODO the qualified names are not relevant in the seq, so we just use dummy::
    return SeqToken.dummy + SeqSyntax.COLON + SeqSyntax.COLON + pVarName;
  }

  public static String buildOutputFileName(Path pInputFilePath) {
    return SeqToken.__MPOR_SEQ__ + getFileNameWithoutExtension(pInputFilePath);
  }

  private static String getFileNameWithoutExtension(Path pInputFilePath) {
    String fileName = pInputFilePath.getFileName().toString();
    return fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
  }
}
