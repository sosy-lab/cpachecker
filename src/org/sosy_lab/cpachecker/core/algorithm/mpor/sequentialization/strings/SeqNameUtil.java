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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.MemoryLocation;
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

  public static String buildDirectDenseBitVectorNameByAccessType(
      MPOROptions pOptions, int pThreadId, BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("cannot build name for NONE access type");
      case ACCESS -> buildDenseBitVectorAccessName(pOptions, true, pThreadId);
      case READ -> buildDenseBitVectorReadName(pOptions, true, pThreadId);
      case WRITE -> buildDenseBitVectorWriteName(pOptions, true, pThreadId);
    };
  }

  public static String buildReachableDenseBitVectorNameByAccessType(
      MPOROptions pOptions, int pThreadId, BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("cannot build name for NONE access type");
      case ACCESS -> buildDenseBitVectorAccessName(pOptions, false, pThreadId);
      case READ -> buildDenseBitVectorReadName(pOptions, false, pThreadId);
      case WRITE -> buildDenseBitVectorWriteName(pOptions, false, pThreadId);
    };
  }

  // TODO parametrize short / long access type
  private static String buildDenseBitVectorAccessName(
      MPOROptions pOptions, boolean pIsDirect, int pThreadId) {

    return pOptions.shortVariableNames
        ? SeqToken.b + (pIsDirect ? SeqToken.d : SeqToken.r) + SeqToken.a + pThreadId
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + (pIsDirect ? SeqToken.DIRECT : SeqToken.REACHABLE)
            + SeqSyntax.UNDERSCORE
            + SeqToken.ACCESS;
  }

  private static String buildDenseBitVectorReadName(
      MPOROptions pOptions, boolean pIsDirect, int pThreadId) {

    return pOptions.shortVariableNames
        ? SeqToken.b + (pIsDirect ? SeqToken.d : SeqToken.r) + SeqToken.r + pThreadId
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + (pIsDirect ? SeqToken.DIRECT : SeqToken.REACHABLE)
            + SeqSyntax.UNDERSCORE
            + SeqToken.READ;
  }

  private static String buildDenseBitVectorWriteName(
      MPOROptions pOptions, boolean pIsDirect, int pThreadId) {

    return pOptions.shortVariableNames
        ? SeqToken.b + (pIsDirect ? SeqToken.d : SeqToken.r) + SeqToken.w + pThreadId
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + (pIsDirect ? SeqToken.DIRECT : SeqToken.REACHABLE)
            + SeqSyntax.UNDERSCORE
            + SeqToken.WRITE;
  }

  public static String buildLastReachableDenseBitVectorNameByAccessType(
      MPOROptions pOptions, BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("cannot build name for NONE access type");
      case ACCESS -> buildLastDenseBitVectorName(pOptions, SeqToken.a, SeqToken.ACCESS);
      case READ -> buildLastDenseBitVectorName(pOptions, SeqToken.r, SeqToken.READ);
      case WRITE -> buildLastDenseBitVectorName(pOptions, SeqToken.w, SeqToken.WRITE);
    };
  }

  private static String buildLastDenseBitVectorName(
      MPOROptions pOptions, String pAccessTypeShort, String pAccessTypeLong) {

    return pOptions.shortVariableNames
        ? SeqToken.last + SeqSyntax.UNDERSCORE + SeqToken.b + SeqToken.r + pAccessTypeShort
        : SeqToken.LAST
            + SeqSyntax.UNDERSCORE
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + SeqToken.REACHABLE
            + SeqSyntax.UNDERSCORE
            + pAccessTypeLong;
  }

  // Sparse Bit Vector =============================================================================

  public static String buildSparseBitVectorAccessVariableName(
      MPOROptions pOptions, int pThreadId, MemoryLocation pMemoryLocation) {

    String variableName = pMemoryLocation.getName();
    return pOptions.shortVariableNames
        ? SeqToken.b + SeqToken.a + pThreadId + SeqSyntax.UNDERSCORE + variableName
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + SeqToken.ACCESS
            + SeqSyntax.UNDERSCORE
            + variableName;
  }

  public static String buildSparseBitVectorReadVariableName(
      MPOROptions pOptions, int pThreadId, MemoryLocation pMemoryLocation) {

    String variableName = pMemoryLocation.getName();
    return pOptions.shortVariableNames
        ? SeqToken.b + SeqToken.r + pThreadId + SeqSyntax.UNDERSCORE + variableName
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + SeqToken.READ
            + SeqSyntax.UNDERSCORE
            + variableName;
  }

  public static String buildSparseBitVectorWriteVariableName(
      MPOROptions pOptions, int pThreadId, MemoryLocation pMemoryLocation) {

    String variableName = pMemoryLocation.getName();
    return pOptions.shortVariableNames
        ? SeqToken.b + SeqToken.w + pThreadId + SeqSyntax.UNDERSCORE + variableName
        : buildThreadPrefix(pOptions, pThreadId)
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + SeqToken.WRITE
            + SeqSyntax.UNDERSCORE
            + variableName;
  }

  public static String buildLastReachableSparseBitVectorNameByAccessType(
      MPOROptions pOptions, MemoryLocation pVariableDeclaration, BitVectorAccessType pAccessType) {

    String variableName = pVariableDeclaration.getName();
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("cannot build name for NONE access type");
      case ACCESS ->
          buildLastSparseBitVectorName(pOptions, SeqToken.a, SeqToken.ACCESS, variableName);
      case READ -> buildLastSparseBitVectorName(pOptions, SeqToken.r, SeqToken.READ, variableName);
      case WRITE ->
          buildLastSparseBitVectorName(pOptions, SeqToken.w, SeqToken.WRITE, variableName);
    };
  }

  private static String buildLastSparseBitVectorName(
      MPOROptions pOptions, String pAccessTypeShort, String pAccessTypeLong, String pVariableName) {

    return pOptions.shortVariableNames
        ? SeqToken.last
            + SeqSyntax.UNDERSCORE
            + SeqToken.b
            + SeqToken.r
            + pAccessTypeShort
            + SeqSyntax.UNDERSCORE
            + pVariableName
        : SeqToken.LAST
            + SeqSyntax.UNDERSCORE
            + SeqToken.BIT_VECTOR
            + SeqSyntax.UNDERSCORE
            + SeqToken.REACHABLE
            + SeqSyntax.UNDERSCORE
            + pAccessTypeLong
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
