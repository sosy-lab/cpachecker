// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.file.Path;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqNameUtil {

  private static int variableId = 0;

  private static int loopHeadId = 0;

  private static String createVariableId() {
    return SeqSyntax.UNDERSCORE + variableId++ + SeqSyntax.UNDERSCORE;
  }

  private static String buildThreadPrefix(MPOROptions pOptions, int pThreadId) {
    return (pOptions.shortVariables ? SeqToken.T : SeqToken.__MPOR_SEQ__ + SeqToken.THREAD)
        + pThreadId
        + SeqSyntax.UNDERSCORE;
  }

  /** Returns {@code "__MPOR_SEQ__{pFunctionName}"}. */
  public static String buildFunctionName(String pFunctionName) {
    return SeqToken.__MPOR_SEQ__ + pFunctionName;
  }

  public static String buildThreadAssumeLabelName(MPOROptions pOptions, int pThreadId) {
    return buildThreadPrefix(pOptions, pThreadId) + SeqToken.ASSUME;
  }

  public static String buildThreadSwitchLabelName(MPOROptions pOptions, int pThreadId) {
    return buildThreadPrefix(pOptions, pThreadId) + SeqToken.SWITCH;
  }

  public static String buildLoopHeadLabelName(MPOROptions pOptions, int pThreadId) {
    return buildThreadPrefix(pOptions, pThreadId)
        + SeqToken.LOOP_HEAD
        + SeqSyntax.UNDERSCORE
        + loopHeadId++;
  }

  public static String buildGlobalVariableName(
      MPOROptions pOptions, CVariableDeclaration pVariableDeclaration) {

    checkArgument(pVariableDeclaration.isGlobal(), "variable declaration must be global");
    return buildGlobalVariablePrefix(pOptions)
        + createVariableId()
        + pVariableDeclaration.getName();
  }

  public static String buildLocalVariableName(
      MPOROptions pOptions,
      CVariableDeclaration pVariableDeclaration,
      int pThreadId,
      int pCallContext,
      Optional<String> pFunctionName) {

    String functionName =
        pFunctionName.isPresent()
            ? pFunctionName.orElseThrow() + SeqSyntax.UNDERSCORE
            : SeqSyntax.EMPTY_STRING;
    String prefix = buildLocalVariablePrefix(pOptions, functionName, pThreadId, pCallContext);
    return prefix + createVariableId() + pVariableDeclaration.getName();
  }

  private static String buildGlobalVariablePrefix(MPOROptions pOptions) {
    return pOptions.shortVariables ? SeqToken.G : SeqToken.GLOBAL;
  }

  private static String buildLocalVariablePrefix(
      MPOROptions pOptions, String pFunctionName, int pThreadId, int pCallContext) {

    return (pOptions.shortVariables ? SeqToken.L : SeqToken.LOCAL)
        + SeqSyntax.UNDERSCORE
        + pFunctionName
        + (pOptions.shortVariables ? SeqToken.T : SeqToken.THREAD)
        + pThreadId
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariables ? SeqToken.C : SeqToken.CALL)
        + pCallContext;
  }

  /** Returns a var name of the form {@code PARAM_{...}_variable}. */
  public static String buildParameterName(
      MPOROptions pOptions,
      CParameterDeclaration pParameterDeclaration,
      int pThreadId,
      String pFunctionName,
      int pCallNumber) {

    return (pOptions.shortVariables ? SeqToken.P : SeqToken.PARAMETER)
        + SeqSyntax.UNDERSCORE
        + pFunctionName
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariables ? SeqToken.T : SeqToken.THREAD)
        + pThreadId
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariables ? SeqToken.C : SeqToken.CALL)
        + pCallNumber
        + createVariableId()
        + pParameterDeclaration.getName();
  }

  public static String buildStartRoutineArgName(
      MPOROptions pOptions,
      // TODO make name (String) parameter directly
      CParameterDeclaration pStartRoutineArgDeclaration,
      int pThreadId,
      String pFunctionName) {

    return (pOptions.shortVariables ? SeqToken.S : SeqToken.START_ROUTINE_ARG)
        + SeqSyntax.UNDERSCORE
        + pFunctionName
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariables ? SeqToken.T : SeqToken.THREAD)
        + pThreadId
        + createVariableId()
        + pStartRoutineArgDeclaration.getName();
  }

  public static String buildIntermediateExitVariableName(MPOROptions pOptions, int pThreadId) {
    return (pOptions.shortVariables ? SeqToken.E : SeqToken.EXIT)
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariables ? SeqToken.T : SeqToken.THREAD)
        + pThreadId
        + createVariableId()
        + SeqToken.return_value;
  }

  public static String buildBitVectorName(MPOROptions pOptions, int pThreadId) {
    return pOptions.shortVariables
        ? SeqToken.b + pThreadId
        : buildThreadPrefix(pOptions, pThreadId) + SeqToken.BIT_VECTOR;
  }

  public static String buildBitVectorScalarAccessVariableName(
      MPOROptions pOptions, int pThreadId, CVariableDeclaration pVariableDeclaration) {

    checkArgument(pVariableDeclaration.isGlobal(), "pVariableDeclaration must be global");
    String variableName = pVariableDeclaration.getName();
    return pOptions.shortVariables
        ? SeqToken.b + pThreadId + SeqSyntax.UNDERSCORE + variableName
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + variableName;
  }

  /** Returns a var name of the form {@code __MPOR_SEQ__{pMutexName}_LOCKED} */
  public static String buildMutexLockedName(MPOROptions pOptions, String pMutexName) {
    return (pOptions.shortVariables ? pMutexName : SeqToken.__MPOR_SEQ__ + pMutexName)
        + SeqSyntax.UNDERSCORE
        + SeqToken.LOCKED;
  }

  // TODO this should be more distinct from LOCKED, maybe T0_REQUESTS_lock?
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
        + (pOptions.shortVariables ? SeqToken.T : SeqToken.THREAD)
        + pTargetId;
  }

  public static String buildAtomicLockedName(MPOROptions pOptions) {
    return (pOptions.shortVariables
            ? SeqToken.__MPOR_SEQ__ + SeqToken.ATOMIC
            : SeqToken.__MPOR_SEQ__)
        + SeqSyntax.UNDERSCORE
        + SeqToken.LOCKED;
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
