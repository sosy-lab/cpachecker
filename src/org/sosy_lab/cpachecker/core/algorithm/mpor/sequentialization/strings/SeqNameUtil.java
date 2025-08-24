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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqNameUtil {

  public static String buildThreadKVariable(int pThreadId) {
    return SeqToken.K + pThreadId;
  }

  public static String buildThreadPrefix(MPOROptions pOptions, int pThreadId) {
    return (pOptions.shortVariableNames ? SeqToken.T : SeqToken.__MPOR_SEQ__ + SeqToken.THREAD)
        + pThreadId
        + SeqSyntax.UNDERSCORE;
  }

  /** Returns {@code "__MPOR_SEQ__{pFunctionName}"}. */
  public static String buildFunctionName(String pFunctionName) {
    return SeqToken.__MPOR_SEQ__ + pFunctionName;
  }

  public static String buildGlobalVariableName(
      MPOROptions pOptions, CVariableDeclaration pVariableDeclaration) {

    checkArgument(pVariableDeclaration.isGlobal(), "variable declaration must be global");
    return buildGlobalVariablePrefix(pOptions)
        + SeqSyntax.UNDERSCORE
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
    return prefix + SeqSyntax.UNDERSCORE + pVariableDeclaration.getName();
  }

  private static String buildGlobalVariablePrefix(MPOROptions pOptions) {
    return pOptions.shortVariableNames ? SeqToken.G : SeqToken.GLOBAL;
  }

  private static String buildLocalVariablePrefix(
      MPOROptions pOptions, String pFunctionName, int pThreadId, int pCallContext) {

    return (pOptions.shortVariableNames ? SeqToken.L : SeqToken.LOCAL)
        + SeqSyntax.UNDERSCORE
        + pFunctionName
        + (pOptions.shortVariableNames ? SeqToken.T : SeqToken.THREAD)
        + pThreadId
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariableNames ? SeqToken.C : SeqToken.CALL)
        + pCallContext;
  }

  /** Returns a var name of the form {@code PARAM_{...}_variable}. */
  public static String buildParameterName(
      MPOROptions pOptions,
      CParameterDeclaration pParameterDeclaration,
      int pThreadId,
      String pFunctionName,
      int pCallNumber) {

    return (pOptions.shortVariableNames ? SeqToken.P : SeqToken.PARAMETER)
        + SeqSyntax.UNDERSCORE
        + pFunctionName
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariableNames ? SeqToken.T : SeqToken.THREAD)
        + pThreadId
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariableNames ? SeqToken.C : SeqToken.CALL)
        + pCallNumber
        + SeqSyntax.UNDERSCORE
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

    return (pOptions.shortVariableNames ? SeqToken.M : SeqToken.MAIN_FUNCTION_ARG)
        + SeqSyntax.UNDERSCORE
        + pMainFunctionArgDeclaration.getName();
  }

  public static String buildStartRoutineArgName(
      MPOROptions pOptions,
      // TODO make name (String) parameter directly
      CParameterDeclaration pStartRoutineArgDeclaration,
      int pThreadId,
      String pFunctionName) {

    return (pOptions.shortVariableNames ? SeqToken.S : SeqToken.START_ROUTINE_ARG)
        + SeqSyntax.UNDERSCORE
        + pFunctionName
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariableNames ? SeqToken.T : SeqToken.THREAD)
        + pThreadId
        + SeqSyntax.UNDERSCORE
        + pStartRoutineArgDeclaration.getName();
  }

  public static String buildStartRoutineExitVariableName(MPOROptions pOptions, int pThreadId) {
    return (pOptions.shortVariableNames ? SeqToken.E : SeqToken.EXIT)
        + SeqSyntax.UNDERSCORE
        + (pOptions.shortVariableNames ? SeqToken.T : SeqToken.THREAD)
        + pThreadId
        + SeqSyntax.UNDERSCORE
        + SeqToken.return_value;
  }

  // Dense Bit Vectors =============================================================================

  public static String buildDenseBitVectorNameByAccessType(
      MPOROptions pOptions, boolean pIsDirect, int pThreadId, MemoryAccessType pAccessType) {

    return pOptions.shortVariableNames
        ? SeqToken.b + (pIsDirect ? SeqToken.d : SeqToken.r) + pAccessType.shortName + pThreadId
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + (pIsDirect ? SeqToken.DIRECT : SeqToken.REACHABLE)
            + SeqSyntax.UNDERSCORE
            + pAccessType.longName;
  }

  public static String buildLastReachableDenseBitVectorNameByAccessType(
      MPOROptions pOptions, MemoryAccessType pAccessType) {

    return pOptions.shortVariableNames
        ? SeqToken.last + SeqSyntax.UNDERSCORE + SeqToken.b + SeqToken.r + pAccessType.shortName
        : SeqToken.LAST
            + SeqSyntax.UNDERSCORE
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + SeqToken.REACHABLE
            + SeqSyntax.UNDERSCORE
            + pAccessType.longName;
  }

  // Sparse Bit Vector =============================================================================

  public static String buildSparseBitVectorNameByAccessType(
      MPOROptions pOptions,
      int pThreadId,
      MemoryLocation pMemoryLocation,
      MemoryAccessType pAccessType) {

    String variableName = pMemoryLocation.getName();
    return pOptions.shortVariableNames
        ? SeqToken.b + pAccessType.shortName + pThreadId + SeqSyntax.UNDERSCORE + variableName
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + pAccessType.longName
            + SeqSyntax.UNDERSCORE
            + variableName;
  }

  public static String buildLastSparseBitVectorNameByAccessType(
      MPOROptions pOptions, String pVariableName, MemoryAccessType pAccessType) {

    return pOptions.shortVariableNames
        ? SeqToken.last
            + SeqSyntax.UNDERSCORE
            + SeqToken.b
            + SeqToken.r
            + pAccessType.shortName
            + SeqSyntax.UNDERSCORE
            + pVariableName
        : SeqToken.LAST
            + SeqSyntax.UNDERSCORE
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + SeqToken.REACHABLE
            + SeqSyntax.UNDERSCORE
            + pAccessType.longName
            + SeqSyntax.UNDERSCORE
            + pVariableName;
  }

  // Mutex =========================================================================================

  /** Returns a var name of the form {@code __MPOR_SEQ__{pMutexName}_LOCKED} */
  public static String buildMutexLockedName(MPOROptions pOptions, String pMutexName) {
    return (pOptions.shortVariableNames ? pMutexName : SeqToken.__MPOR_SEQ__ + pMutexName)
        + SeqSyntax.UNDERSCORE
        + SeqToken.LOCKED;
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
