// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings;

import java.nio.file.Path;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqNameUtil {

  private static int variableId = 0;

  private static String createVariableId() {
    return SeqSyntax.UNDERSCORE + variableId++ + SeqSyntax.UNDERSCORE;
  }

  private static String buildThreadPrefix(MPOROptions pOptions, int pThreadId) {
    if (pOptions.shortVariables) {
      return SeqToken.T + pThreadId + SeqSyntax.UNDERSCORE;
    } else {
      return SeqToken.__MPOR_SEQ__ + SeqToken.THREAD + pThreadId + SeqSyntax.UNDERSCORE;
    }
  }

  /** Returns {@code "__MPOR_SEQ__{pFunctionName}"}. */
  public static String buildFunctionName(String pFunctionName) {
    return SeqToken.__MPOR_SEQ__ + pFunctionName;
  }

  /**
   * Returns a var name of the form {@code GLOBAL_{...}_variable} for global variables and {@code
   * LOCAL_{...}_variable} for thread local variables.
   */
  public static String buildVariableName(
      MPOROptions pOptions,
      CVariableDeclaration pVariableDeclaration,
      int pThreadId,
      int pCallContext,
      Optional<String> pFunctionName) {

    String functionName =
        pFunctionName.isPresent()
            ? pFunctionName.orElseThrow() + SeqSyntax.UNDERSCORE
            : SeqSyntax.EMPTY_STRING;
    String prefix;
    if (pVariableDeclaration.isGlobal()) {
      prefix = buildGlobalVariablePrefix(pOptions);
    } else {
      prefix = buildLocalVariablePrefix(pOptions, functionName, pThreadId, pCallContext);
    }
    return prefix + createVariableId() + pVariableDeclaration.getName();
  }

  private static String buildGlobalVariablePrefix(MPOROptions pOptions) {
    return pOptions.shortVariables ? SeqToken.G : SeqToken.GLOBAL;
  }

  private static String buildLocalVariablePrefix(
      MPOROptions pOptions, String pFunctionName, int pThreadId, int pCallContext) {

    if (pOptions.shortVariables) {
      return SeqToken.L
          + SeqSyntax.UNDERSCORE
          + pFunctionName
          + SeqToken.T
          + pThreadId
          + SeqSyntax.UNDERSCORE
          + SeqToken.C
          + pCallContext;
    } else {
      return SeqToken.LOCAL
          + SeqSyntax.UNDERSCORE
          + pFunctionName
          + SeqToken.THREAD
          + pThreadId
          + SeqSyntax.UNDERSCORE
          + SeqToken.CALL
          + pCallContext;
    }
  }

  /** Returns a var name of the form {@code PARAM_{...}_variable}. */
  public static String buildParameterName(
      MPOROptions pOptions,
      CParameterDeclaration pParameterDeclaration,
      int pThreadId,
      String pFunctionName,
      int pCallNumber) {

    if (pOptions.shortVariables) {
      return SeqToken.P
          + SeqSyntax.UNDERSCORE
          + pFunctionName
          + SeqSyntax.UNDERSCORE
          + SeqToken.T
          + SeqSyntax.UNDERSCORE
          + SeqToken.C
          + pCallNumber
          + createVariableId()
          + pParameterDeclaration.getName();
    } else {
      return SeqToken.PARAMETER
          + SeqSyntax.UNDERSCORE
          + pFunctionName
          + SeqSyntax.UNDERSCORE
          + SeqToken.THREAD
          + pThreadId
          + SeqSyntax.UNDERSCORE
          + SeqToken.CALL
          + pCallNumber
          + createVariableId()
          + pParameterDeclaration.getName();
    }
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__{pMutexName}_LOCKED} */
  public static String buildMutexLockedName(MPOROptions pOptions, String pMutexName) {
    if (pOptions.shortVariables) {
      return pMutexName + SeqSyntax.UNDERSCORE + SeqToken.LOCKED;
    } else {
      return SeqToken.__MPOR_SEQ__ + pMutexName + SeqSyntax.UNDERSCORE + SeqToken.LOCKED;
    }
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__THREAD{pThreadId}_AWAITS_{pMutexName}} */
  public static String buildThreadLocksMutexName(
      MPOROptions pOptions, int pThreadId, String pMutexName) {

    return buildThreadPrefix(pOptions, pThreadId)
        + SeqToken.LOCKS
        + SeqSyntax.UNDERSCORE
        + pMutexName;
  }

  /**
   * Returns a var name of the form {@code __MPOR_SEQ__THREAD{pWaitingId}_JOINS_TARGET{pTargetId}}
   */
  public static String buildThreadJoinsThreadName(
      MPOROptions pOptions, int pWaitingId, int pTargetId) {

    return buildThreadPrefix(pOptions, pWaitingId)
        + SeqToken.JOINS
        + SeqSyntax.UNDERSCORE
        + SeqToken.THREAD
        + pTargetId;
  }

  public static String buildAtomicLockedName(MPOROptions pOptions) {
    if (pOptions.shortVariables) {
      return SeqToken.ATOMIC + SeqSyntax.UNDERSCORE + SeqToken.LOCKED;
    } else {
      return SeqToken.__MPOR_SEQ__ + SeqToken.ATOMIC + SeqSyntax.UNDERSCORE + SeqToken.LOCKED;
    }
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__THREAD{pThreadId}_BEGINS_ATOMIC} */
  public static String buildThreadBeginsAtomicName(MPOROptions pOptions, int pThreadId) {
    return buildThreadPrefix(pOptions, pThreadId)
        + SeqToken.BEGINS
        + SeqSyntax.UNDERSCORE
        + SeqToken.ATOMIC;
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
