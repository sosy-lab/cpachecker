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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqNameUtil {

  private static int variableId = 0;

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

  public static String buildSwitchCaseGotoLabelPrefix(MPOROptions pOptions, int pThreadId) {

    return buildThreadPrefix(pOptions, pThreadId);
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

  public static String buildParameterNameForEmptyFunctionDefinition(
      CFunctionDeclaration pFunctionDeclaration, int pParameterNumber) {

    return pFunctionDeclaration.getOrigName()
        + SeqSyntax.UNDERSCORE
        + SeqToken.PARAMETER
        + pParameterNumber;
  }

  public static String buildMainFunctionArgName(
      MPOROptions pOptions, CParameterDeclaration pMainFunctionArgDeclaration) {

    return (pOptions.shortVariables ? SeqToken.M : SeqToken.MAIN_FUNCTION_ARG)
        + createVariableId()
        + pMainFunctionArgDeclaration.getName();
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

  public static String buildStartRoutineExitVariableName(MPOROptions pOptions, int pThreadId) {
    return (pOptions.shortVariables ? SeqToken.E : SeqToken.EXIT)
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariables ? SeqToken.T : SeqToken.THREAD)
        + pThreadId
        + createVariableId()
        + SeqToken.return_value;
  }

  // Dense Bit Vectors =============================================================================

  public static String buildDirectBitVectorNameByAccessType(
      MPOROptions pOptions, int pThreadId, BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("cannot build name for NONE access type");
      case ACCESS -> buildBitVectorAccessName(pOptions, true, pThreadId);
      case READ -> buildBitVectorReadName(pOptions, true, pThreadId);
      case WRITE -> buildBitVectorWriteName(pOptions, true, pThreadId);
    };
  }

  public static String buildReachableBitVectorNameByAccessType(
      MPOROptions pOptions, int pThreadId, BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("cannot build name for NONE access type");
      case ACCESS -> buildBitVectorAccessName(pOptions, false, pThreadId);
      case READ -> buildBitVectorReadName(pOptions, false, pThreadId);
      case WRITE -> buildBitVectorWriteName(pOptions, false, pThreadId);
    };
  }

  private static String buildBitVectorAccessName(
      MPOROptions pOptions, boolean pIsDirect, int pThreadId) {

    return pOptions.shortVariables
        ? (pIsDirect ? SeqToken.d : SeqSyntax.EMPTY_STRING) + SeqToken.ba + pThreadId
        : buildThreadPrefix(pOptions, pThreadId)
            + (pIsDirect ? SeqToken.DIRECT + SeqSyntax.UNDERSCORE : SeqSyntax.EMPTY_STRING)
            + SeqToken.BIT_VECTOR_ACCESS;
  }

  private static String buildBitVectorReadName(
      MPOROptions pOptions, boolean pIsDirect, int pThreadId) {

    return pOptions.shortVariables
        ? (pIsDirect ? SeqToken.d : SeqSyntax.EMPTY_STRING) + SeqToken.br + pThreadId
        : buildThreadPrefix(pOptions, pThreadId)
            + (pIsDirect ? SeqToken.DIRECT + SeqSyntax.UNDERSCORE : SeqSyntax.EMPTY_STRING)
            + SeqToken.BIT_VECTOR_READ;
  }

  private static String buildBitVectorWriteName(
      MPOROptions pOptions, boolean pIsDirect, int pThreadId) {

    return pOptions.shortVariables
        ? (pIsDirect ? SeqToken.d : SeqSyntax.EMPTY_STRING) + SeqToken.bw + pThreadId
        : buildThreadPrefix(pOptions, pThreadId)
            + (pIsDirect ? SeqToken.DIRECT + SeqSyntax.UNDERSCORE : SeqSyntax.EMPTY_STRING)
            + SeqToken.BIT_VECTOR_WRITE;
  }

  // Scalar Bit Vector =============================================================================

  public static String buildBitVectorScalarAccessVariableName(
      MPOROptions pOptions, int pThreadId, CVariableDeclaration pVariableDeclaration) {

    checkArgument(pVariableDeclaration.isGlobal(), "pVariableDeclaration must be global");
    String variableName = pVariableDeclaration.getName();
    return pOptions.shortVariables
        ? SeqToken.ba + pThreadId + SeqSyntax.UNDERSCORE + variableName
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR_ACCESS
            + SeqSyntax.UNDERSCORE
            + variableName;
  }

  public static String buildBitVectorScalarReadVariableName(
      MPOROptions pOptions, int pThreadId, CVariableDeclaration pVariableDeclaration) {

    checkArgument(pVariableDeclaration.isGlobal(), "pVariableDeclaration must be global");
    String variableName = pVariableDeclaration.getName();
    return pOptions.shortVariables
        ? SeqToken.br + pThreadId + SeqSyntax.UNDERSCORE + variableName
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR_READ
            + SeqSyntax.UNDERSCORE
            + variableName;
  }

  public static String buildBitVectorScalarWriteVariableName(
      MPOROptions pOptions, int pThreadId, CVariableDeclaration pVariableDeclaration) {

    checkArgument(pVariableDeclaration.isGlobal(), "pVariableDeclaration must be global");
    String variableName = pVariableDeclaration.getName();
    return pOptions.shortVariables
        ? SeqToken.bw + pThreadId + SeqSyntax.UNDERSCORE + variableName
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR_WRITE
            + SeqSyntax.UNDERSCORE
            + variableName;
  }

  // Mutex =========================================================================================

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
