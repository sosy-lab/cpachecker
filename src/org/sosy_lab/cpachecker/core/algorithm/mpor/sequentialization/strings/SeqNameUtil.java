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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.SeqBitVectorDirection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqNameUtil {

  public static String buildThreadPrefix(MPOROptions pOptions, int pThreadId) {
    return (pOptions.shortVariableNames() ? "T" : "THREAD") + pThreadId;
  }

  public static String buildThreadStatementBlockLabelName(int pThreadId, int pLabelNumber) {
    return "T" + pThreadId + "_" + pLabelNumber;
  }

  private static String buildCallSuffix(MPOROptions pOptions, int pCallNumber) {
    return (pOptions.shortVariableNames() ? "C" : "CALL") + pCallNumber;
  }

  public static String buildGlobalVariableName(
      MPOROptions pOptions, CVariableDeclaration pVariableDeclaration) {

    checkArgument(pVariableDeclaration.isGlobal(), "variable declaration must be global");
    return buildGlobalVariablePrefix(pOptions) + "_" + pVariableDeclaration.getName();
  }

  public static String buildLocalVariableName(
      MPOROptions pOptions,
      CVariableDeclaration pVariableDeclaration,
      int pThreadId,
      int pCallContext,
      Optional<String> pFunctionName) {

    String functionName = pFunctionName.isPresent() ? pFunctionName.orElseThrow() + "_" : "";
    String prefix = buildLocalVariablePrefix(pOptions, functionName, pThreadId, pCallContext);
    return prefix + "_" + pVariableDeclaration.getName();
  }

  private static String buildGlobalVariablePrefix(MPOROptions pOptions) {
    return pOptions.shortVariableNames() ? "G" : "GLOBAL";
  }

  private static String buildLocalVariablePrefix(
      MPOROptions pOptions, String pFunctionName, int pThreadId, int pCallNumber) {

    return Joiner.on("_")
        .join(
            (pOptions.shortVariableNames() ? "L" : "LOCAL"),
            pFunctionName,
            buildThreadPrefix(pOptions, pThreadId),
            buildCallSuffix(pOptions, pCallNumber));
  }

  public static String buildSubstituteParameterDeclarationName(
      MPOROptions pOptions,
      CParameterDeclaration pParameterDeclaration,
      int pThreadId,
      String pFunctionName,
      int pCallNumber,
      int pArgumentIndex) {

    return Joiner.on("_")
        .join(
            pOptions.shortVariableNames() ? "P" : "PARAMETER",
            pFunctionName,
            buildThreadPrefix(pOptions, pThreadId),
            buildCallSuffix(pOptions, pCallNumber),
            pParameterDeclaration.getName(),
            (pOptions.shortVariableNames() ? "A" : "ARGUMENT") + pArgumentIndex);
  }

  public static String buildMainFunctionArgName(
      MPOROptions pOptions, CParameterDeclaration pMainFunctionArgDeclaration) {

    return (pOptions.shortVariableNames() ? "M_" : "MAIN_FUNCTION_ARG_")
        + pMainFunctionArgDeclaration.getName();
  }

  public static String buildStartRoutineArgName(
      MPOROptions pOptions,
      // TODO make name (String) parameter directly
      CParameterDeclaration pStartRoutineArgDeclaration,
      int pThreadId,
      String pFunctionName) {

    String startPrefix = pOptions.shortVariableNames() ? "S" : "START_ROUTINE_ARG";
    String threadPrefix = buildThreadPrefix(pOptions, pThreadId);
    return Joiner.on("_")
        .join(startPrefix, pFunctionName, threadPrefix, pStartRoutineArgDeclaration.getName());
  }

  public static String buildStartRoutineExitVariableName(MPOROptions pOptions, int pThreadId) {
    String exitPrefix = pOptions.shortVariableNames() ? "E" : "EXIT";
    String threadPrefix = buildThreadPrefix(pOptions, pThreadId);
    return Joiner.on("_").join(exitPrefix, threadPrefix, "RETURN_VALUE");
  }

  // Bit Vectors ===================================================================================

  public static String buildBitVectorName(
      MPOROptions pOptions,
      Optional<MPORThread> pThread,
      Optional<SeqMemoryLocation> pMemoryLocation,
      MemoryAccessType pAccessType,
      ReachType pReachType,
      SeqBitVectorDirection pDirection) {

    return switch (pDirection) {
      case CURRENT ->
          buildBitVectorName(
              pOptions, pThread.orElseThrow().id(), pMemoryLocation, pAccessType, pReachType);
      case LAST -> buildLastBitVectorName(pOptions, pMemoryLocation, pAccessType);
    };
  }

  private static String buildBitVectorName(
      MPOROptions pOptions,
      int pThreadId,
      Optional<SeqMemoryLocation> pMemoryLocation,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              "Cannot build name, bitVectorEncoding is " + pOptions.bitVectorEncoding());
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

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              "Cannot build name, bitVectorEncoding is " + pOptions.bitVectorEncoding());
      case BINARY, DECIMAL, HEXADECIMAL -> buildLastDenseBitVectorName(pOptions, pAccessType);
      case SPARSE ->
          buildLastSparseBitVectorName(pOptions, pMemoryLocation.orElseThrow(), pAccessType);
    };
  }

  // Dense Bit Vectors =============================================================================

  private static String buildDenseBitVectorName(
      MPOROptions pOptions, int pThreadId, MemoryAccessType pAccessType, ReachType pReachType) {

    return pOptions.shortVariableNames()
        ? "b" + pReachType.shortName + pAccessType.shortName + pThreadId
        : Joiner.on("_")
            .join(
                buildThreadPrefix(pOptions, pThreadId),
                "BIT_VECTOR",
                pReachType.longName,
                pAccessType.longName);
  }

  private static String buildLastDenseBitVectorName(
      MPOROptions pOptions, MemoryAccessType pAccessType) {

    // last bit vectors are always reachable
    return pOptions.shortVariableNames()
        ? "last_b" + ReachType.REACHABLE.shortName + pAccessType.shortName
        : Joiner.on("_")
            .join("LAST_BIT_VECTOR", ReachType.REACHABLE.longName, pAccessType.longName);
  }

  // Sparse Bit Vector =============================================================================

  private static String buildSparseBitVectorName(
      MPOROptions pOptions,
      int pThreadId,
      SeqMemoryLocation pMemoryLocation,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    return pOptions.shortVariableNames()
        ? "b"
            + pReachType.shortName
            + pAccessType.shortName
            + pThreadId
            + "_"
            + pMemoryLocation.getName()
        : Joiner.on("_")
            .join(
                buildThreadPrefix(pOptions, pThreadId),
                "BIT_VECTOR",
                pReachType.longName,
                pAccessType.longName,
                pMemoryLocation.getName());
  }

  private static String buildLastSparseBitVectorName(
      MPOROptions pOptions, SeqMemoryLocation pMemoryLocation, MemoryAccessType pAccessType) {

    return pOptions.shortVariableNames()
        ? "last_b"
            + ReachType.REACHABLE.shortName
            + pAccessType.shortName
            + "_"
            + pMemoryLocation.getName()
        : Joiner.on("_")
            .join(
                "LAST_BIT_VECTOR",
                ReachType.REACHABLE.longName,
                pAccessType.longName,
                pMemoryLocation.getName());
  }

  // Other =========================================================================================

  public static String buildDummyQualifiedName(String pVariableName) {
    // the qualified names are not relevant in the seq, so we just use dummy::
    return "dummy::" + pVariableName;
  }
}
