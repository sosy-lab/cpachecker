// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import java.nio.file.Path;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorDirection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqNameUtil {

  public static String buildThreadPrefix(MPOROptions pOptions, int pThreadId) {
    return (pOptions.shortVariableNames ? SeqToken.T : SeqToken.THREAD) + pThreadId;
  }

  public static String buildFunctionName(String pFunctionName) {
    return SeqToken.MPOR_PREFIX + pFunctionName;
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
      MPOROptions pOptions, String pFunctionName, int pThreadId, int pCallNumber) {

    return Joiner.on(SeqSyntax.UNDERSCORE)
        .join(
            (pOptions.shortVariableNames ? SeqToken.L : SeqToken.LOCAL),
            pFunctionName,
            buildThreadPrefix(pOptions, pThreadId),
            (pOptions.shortVariableNames ? SeqToken.C : SeqToken.CALL) + pCallNumber);
  }

  public static String buildParameterName(
      MPOROptions pOptions,
      String pFunctionName,
      int pThreadId,
      int pCallNumber,
      CParameterDeclaration pParameterDeclaration,
      int pArgumentIndex) {

    return Joiner.on(SeqSyntax.UNDERSCORE)
        .join(
            pOptions.shortVariableNames ? SeqToken.P : SeqToken.PARAMETER,
            pFunctionName,
            buildThreadPrefix(pOptions, pThreadId),
            (pOptions.shortVariableNames ? SeqToken.C : SeqToken.CALL) + pCallNumber,
            pParameterDeclaration.getName(),
            (pOptions.shortVariableNames ? SeqToken.A : SeqToken.ARG) + pArgumentIndex);
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

    String startPrefix = pOptions.shortVariableNames ? SeqToken.S : SeqToken.START_ROUTINE_ARG;
    String threadPrefix = buildThreadPrefix(pOptions, pThreadId);
    return Joiner.on(SeqSyntax.UNDERSCORE)
        .join(startPrefix, pFunctionName, threadPrefix, pStartRoutineArgDeclaration.getName());
  }

  public static String buildStartRoutineExitVariableName(MPOROptions pOptions, int pThreadId) {
    String exitPrefix = pOptions.shortVariableNames ? SeqToken.E : SeqToken.EXIT;
    String threadPrefix = buildThreadPrefix(pOptions, pThreadId);
    return Joiner.on(SeqSyntax.UNDERSCORE).join(exitPrefix, threadPrefix, SeqToken.return_value);
  }

  // Bit Vectors ===================================================================================

  public static String buildBitVectorName(
      MPOROptions pOptions,
      Optional<MPORThread> pThread,
      Optional<SeqMemoryLocation> pMemoryLocation,
      MemoryAccessType pAccessType,
      ReachType pReachType,
      BitVectorDirection pDirection) {

    return switch (pDirection) {
      case CURRENT ->
          buildBitVectorName(
              pOptions, pThread.orElseThrow().getId(), pMemoryLocation, pAccessType, pReachType);
      case LAST -> buildLastBitVectorName(pOptions, pMemoryLocation, pAccessType);
    };
  }

  private static String buildBitVectorName(
      MPOROptions pOptions,
      int pThreadId,
      Optional<SeqMemoryLocation> pMemoryLocation,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    return switch (pOptions.bitVectorEncoding) {
      case NONE -> throw new IllegalArgumentException();
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildDenseBitVectorName(pOptions, pThreadId, pAccessType, pReachType);
      case SPARSE ->
          buildSparseBitVectorName(
              pOptions, pThreadId, pMemoryLocation.orElseThrow(), pAccessType, pReachType);
    };
  }

  private static String buildLastBitVectorName(
      MPOROptions pOptions,
      Optional<SeqMemoryLocation> pMemoryLocation,
      MemoryAccessType pAccessType) {

    return switch (pOptions.bitVectorEncoding) {
      case NONE -> throw new IllegalArgumentException();
      case BINARY, DECIMAL, HEXADECIMAL -> buildLastDenseBitVectorName(pOptions, pAccessType);
      case SPARSE ->
          buildLastSparseBitVectorName(pOptions, pMemoryLocation.orElseThrow(), pAccessType);
    };
  }

  // Dense Bit Vectors =============================================================================

  private static String buildDenseBitVectorName(
      MPOROptions pOptions, int pThreadId, MemoryAccessType pAccessType, ReachType pReachType) {

    return pOptions.shortVariableNames
        ? SeqToken.b + pReachType.shortName + pAccessType.shortName + pThreadId
        : Joiner.on(SeqSyntax.UNDERSCORE)
            .join(
                buildThreadPrefix(pOptions, pThreadId),
                SeqToken.BIT_VECTOR,
                pReachType.longName,
                pAccessType.longName);
  }

  private static String buildLastDenseBitVectorName(
      MPOROptions pOptions, MemoryAccessType pAccessType) {

    return pOptions.shortVariableNames
        ? SeqToken.last + SeqSyntax.UNDERSCORE + SeqToken.b + SeqToken.round + pAccessType.shortName
        : Joiner.on(SeqSyntax.UNDERSCORE)
            .join(SeqToken.LAST, SeqToken.BIT_VECTOR, SeqToken.REACHABLE, pAccessType.longName);
  }

  // Sparse Bit Vector =============================================================================

  private static String buildSparseBitVectorName(
      MPOROptions pOptions,
      int pThreadId,
      SeqMemoryLocation pMemoryLocation,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    return pOptions.shortVariableNames
        ? SeqToken.b
            + pReachType.shortName
            + pAccessType.shortName
            + pThreadId
            + SeqSyntax.UNDERSCORE
            + pMemoryLocation.getName()
        : Joiner.on(SeqSyntax.UNDERSCORE)
            .join(
                buildThreadPrefix(pOptions, pThreadId),
                SeqToken.BIT_VECTOR,
                pReachType.longName,
                pAccessType.longName,
                pMemoryLocation.getName());
  }

  private static String buildLastSparseBitVectorName(
      MPOROptions pOptions, SeqMemoryLocation pMemoryLocation, MemoryAccessType pAccessType) {

    return pOptions.shortVariableNames
        ? SeqToken.last
            + SeqSyntax.UNDERSCORE
            + SeqToken.b
            + SeqToken.round
            + pAccessType.shortName
            + SeqSyntax.UNDERSCORE
            + pMemoryLocation.getName()
        : Joiner.on(SeqSyntax.UNDERSCORE)
            .join(
                SeqToken.LAST,
                SeqToken.BIT_VECTOR,
                SeqToken.REACHABLE,
                pAccessType.longName,
                pMemoryLocation.getName());
  }

  // Thread Synchronization ========================================================================

  public static String buildCondSignaledName(String pCondName) {
    return pCondName + SeqSyntax.UNDERSCORE + SeqToken.SIGNALED;
  }

  public static String buildMutexLockedName(String pMutexName) {
    return pMutexName + SeqSyntax.UNDERSCORE + SeqToken.LOCKED;
  }

  public static String buildRwLockReadersName(String pRwLockName) {
    return Joiner.on(SeqSyntax.UNDERSCORE).join(pRwLockName, SeqToken.NUM, SeqToken.READERS);
  }

  public static String buildRwLockWritersName(String pRwLockName) {
    return Joiner.on(SeqSyntax.UNDERSCORE).join(pRwLockName, SeqToken.NUM, SeqToken.WRITERS);
  }

  public static String buildSyncName(MPOROptions pOptions, int pThreadId) {
    return buildThreadPrefix(pOptions, pThreadId) + SeqSyntax.UNDERSCORE + SeqToken.SYNC;
  }

  // Other =========================================================================================

  public static String buildDummyQualifiedName(String pVarName) {
    // the qualified names are not relevant in the seq, so we just use dummy::
    return SeqToken.dummy + SeqSyntax.COLON + SeqSyntax.COLON + pVarName;
  }

  public static String buildOutputFileName(String pInputFileName) {
    return SeqToken.MPOR_PREFIX + pInputFileName;
  }

  public static String buildOutputFileName(Path pInputFilePath) {
    return SeqToken.MPOR_PREFIX + getFileNameWithoutExtension(pInputFilePath);
  }

  private static String getFileNameWithoutExtension(Path pInputFilePath) {
    String fileName = pInputFilePath.getFileName().toString();
    return fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
  }
}
