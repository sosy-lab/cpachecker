// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.GlobalVariableFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.PointerAssignments;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorEvaluationBuilder {

  // variable only i.e. no literal expressions =====================================================

  public static BitVectorEvaluationExpression buildVariableOnlyEvaluation(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(
        pOptions.areBitVectorsEnabled(),
        "either conflictReduction or bitVectorReduction must be enabled");

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "conflictReduction must be enabled to build evaluation expression");
      case ACCESS_ONLY ->
          BitVectorAccessEvaluationBuilder.buildVariableOnlyEvaluationByEncoding(
              pOptions,
              pActiveThread,
              pOtherThreads,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
      case READ_AND_WRITE ->
          BitVectorReadWriteEvaluationBuilder.buildVariableOnlyEvaluationByEncoding(
              pOptions,
              pActiveThread,
              pOtherThreads,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
    };
  }

  // last bit vector evaluations (conflict reduction) ==============================================

  public static BitVectorEvaluationExpression buildLastBitVectorEvaluation(
      MPOROptions pOptions,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      PointerAssignments pPointerAssignments,
      SeqThreadStatementBlock pTargetBlock,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(pOptions.conflictReduction, "conflict reduction must be enabled");

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "conflictReduction must be enabled to build evaluation expression");
      case ACCESS_ONLY -> {
        ImmutableSet<CVariableDeclaration> directAccessVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap, pPointerAssignments, pTargetBlock, BitVectorAccessType.ACCESS);
        yield buildLastAccessBitVectorEvaluationByEncoding(
            pOptions, directAccessVariables,
            pBitVectorVariables, pBinaryExpressionBuilder);
      }
      case READ_AND_WRITE -> {
        ImmutableSet<CVariableDeclaration> directReadVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap, pPointerAssignments, pTargetBlock, BitVectorAccessType.READ);
        ImmutableSet<CVariableDeclaration> directWriteVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap, pPointerAssignments, pTargetBlock, BitVectorAccessType.WRITE);
        yield buildLastReadWriteBitVectorEvaluationByEncoding(
            pOptions,
            directReadVariables,
            directWriteVariables,
            pBitVectorVariables,
            pBinaryExpressionBuilder);
      }
    };
  }

  private static BitVectorEvaluationExpression buildLastAccessBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<CVariableDeclaration> pDirectAccessVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL -> {
        LastDenseBitVector lastAccessBitVector =
            pBitVectorVariables.getLastDenseBitVectorByAccessType(BitVectorAccessType.ACCESS);
        ImmutableSet<CExpression> otherAccessBitVectors =
            ImmutableSet.of(lastAccessBitVector.reachableVariable);
        yield BitVectorAccessEvaluationBuilder.buildDenseEvaluation(
            pOptions,
            otherAccessBitVectors,
            pDirectAccessVariables,
            pBitVectorVariables,
            pBinaryExpressionBuilder);
      }
      case SPARSE -> {
        ImmutableListMultimap<CVariableDeclaration, SeqExpression> sparseAccessMap =
            mapGlobalVariablesToLastSparseBitVectorsByAccessType(
                pBitVectorVariables, BitVectorAccessType.ACCESS);
        yield BitVectorAccessEvaluationBuilder.buildSparseEvaluation(
            pOptions, sparseAccessMap, pDirectAccessVariables, pBitVectorVariables);
      }
    };
  }

  private static BitVectorEvaluationExpression buildLastReadWriteBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL -> {
        LastDenseBitVector lastWriteBitVector =
            pBitVectorVariables.getLastDenseBitVectorByAccessType(BitVectorAccessType.WRITE);
        LastDenseBitVector lastAccessBitVector =
            pBitVectorVariables.getLastDenseBitVectorByAccessType(BitVectorAccessType.ACCESS);
        yield BitVectorReadWriteEvaluationBuilder.buildDenseEvaluation(
            pOptions,
            ImmutableSet.of(lastWriteBitVector.reachableVariable),
            ImmutableSet.of(lastAccessBitVector.reachableVariable),
            pDirectReadVariables,
            pDirectWriteVariables,
            pBitVectorVariables,
            pBinaryExpressionBuilder);
      }
      case SPARSE -> {
        ImmutableListMultimap<CVariableDeclaration, SeqExpression> sparseWriteMap =
            mapGlobalVariablesToLastSparseBitVectorsByAccessType(
                pBitVectorVariables, BitVectorAccessType.WRITE);
        ImmutableListMultimap<CVariableDeclaration, SeqExpression> sparseAccessMap =
            mapGlobalVariablesToLastSparseBitVectorsByAccessType(
                pBitVectorVariables, BitVectorAccessType.ACCESS);
        yield BitVectorReadWriteEvaluationBuilder.buildSparseEvaluation(
            pOptions,
            sparseWriteMap,
            sparseAccessMap,
            pDirectReadVariables,
            pDirectWriteVariables,
            pBitVectorVariables);
      }
    };
  }

  private static ImmutableListMultimap<CVariableDeclaration, SeqExpression>
      mapGlobalVariablesToLastSparseBitVectorsByAccessType(
          BitVectorVariables pBitVectorVariables, BitVectorAccessType pAccessType) {

    ImmutableListMultimap.Builder<CVariableDeclaration, SeqExpression> rMap =
        ImmutableListMultimap.builder();
    ImmutableMap<CVariableDeclaration, LastSparseBitVector> lastSparseBitVectors =
        pBitVectorVariables.getLastSparseBitVectorByAccessType(pAccessType);
    for (var entry : lastSparseBitVectors.entrySet()) {
      CVariableDeclaration variableDeclaration = entry.getKey();
      rMap.put(variableDeclaration, new CToSeqExpression(entry.getValue().variable));
    }
    return rMap.build();
  }

  // bit vector evaluations by accessed global variables (bit vector reduction) ====================

  public static BitVectorEvaluationExpression buildEvaluationByDirectVariableAccesses(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      PointerAssignments pPointerAssignments,
      SeqThreadStatementBlock pTargetBlock,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(
        pOptions.bitVectorReduction,
        "bitVectorReduction must be enabled to build evaluation expression");

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "bitVectorReduction must be enabled to build evaluation expression");
      case ACCESS_ONLY -> {
        ImmutableSet<CVariableDeclaration> directAccessVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap, pPointerAssignments, pTargetBlock, BitVectorAccessType.ACCESS);
        yield buildEvaluationByReduction(
            pOptions,
            pOtherThreads,
            directAccessVariables,
            ImmutableSet.of(),
            ImmutableSet.of(),
            pBitVectorVariables,
            pBinaryExpressionBuilder);
      }
      case READ_AND_WRITE -> {
        ImmutableSet<CVariableDeclaration> directReadVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap, pPointerAssignments, pTargetBlock, BitVectorAccessType.READ);
        ImmutableSet<CVariableDeclaration> directWriteVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap, pPointerAssignments, pTargetBlock, BitVectorAccessType.WRITE);
        yield buildEvaluationByReduction(
            pOptions,
            pOtherThreads,
            ImmutableSet.of(),
            directReadVariables,
            directWriteVariables,
            pBitVectorVariables,
            pBinaryExpressionBuilder);
      }
    };
  }

  private static BitVectorEvaluationExpression buildEvaluationByReduction(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectAccessVariables,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "bitVectorReduction must be enabled to build evaluation expression");
      case ACCESS_ONLY ->
          buildAccessEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectAccessVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
      case READ_AND_WRITE ->
          buildReadWriteEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectReadVariables,
              pDirectWriteVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
    };
  }

  private static BitVectorEvaluationExpression buildAccessEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectAccessVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL -> {
        ImmutableSet<CExpression> otherBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                BitVectorAccessType.ACCESS, pOtherThreads);
        yield BitVectorAccessEvaluationBuilder.buildDenseEvaluation(
            pOptions,
            otherBitVectors,
            pDirectAccessVariables,
            pBitVectorVariables,
            pBinaryExpressionBuilder);
      }
      case SPARSE -> {
        ImmutableListMultimap<CVariableDeclaration, SeqExpression> sparseAccessMap =
            mapGlobalVariablesToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, BitVectorAccessType.ACCESS);
        yield BitVectorAccessEvaluationBuilder.buildSparseEvaluation(
            pOptions, sparseAccessMap, pDirectAccessVariables, pBitVectorVariables);
      }
    };
  }

  private static BitVectorEvaluationExpression buildReadWriteEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL -> {
        ImmutableSet<CExpression> otherWriteBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                BitVectorAccessType.WRITE, pOtherThreads);
        ImmutableSet<CExpression> otherAccessBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                BitVectorAccessType.ACCESS, pOtherThreads);
        yield BitVectorReadWriteEvaluationBuilder.buildDenseEvaluation(
            pOptions,
            otherWriteBitVectors,
            otherAccessBitVectors,
            pDirectReadVariables,
            pDirectWriteVariables,
            pBitVectorVariables,
            pBinaryExpressionBuilder);
      }
      case SPARSE -> {
        ImmutableListMultimap<CVariableDeclaration, SeqExpression> sparseWriteMap =
            mapGlobalVariablesToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, BitVectorAccessType.WRITE);
        ImmutableListMultimap<CVariableDeclaration, SeqExpression> sparseAccessMap =
            mapGlobalVariablesToSparseBitVectorsByAccessType(
                pOtherThreads, pBitVectorVariables, BitVectorAccessType.ACCESS);
        yield BitVectorReadWriteEvaluationBuilder.buildSparseEvaluation(
            pOptions,
            sparseWriteMap,
            sparseAccessMap,
            pDirectReadVariables,
            pDirectWriteVariables,
            pBitVectorVariables);
      }
    };
  }

  private static ImmutableListMultimap<CVariableDeclaration, SeqExpression>
      mapGlobalVariablesToSparseBitVectorsByAccessType(
          ImmutableSet<MPORThread> pOtherThreads,
          BitVectorVariables pBitVectorVariables,
          BitVectorAccessType pAccessType) {

    ImmutableListMultimap.Builder<CVariableDeclaration, SeqExpression> rMap =
        ImmutableListMultimap.builder();
    for (var entry : pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      CVariableDeclaration globalVariable = entry.getKey();
      ImmutableMap<MPORThread, CIdExpression> variables = entry.getValue().variables;
      ImmutableList<SeqExpression> otherVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(pOtherThreads, variables);
      rMap.putAll(globalVariable, otherVariables);
    }
    return rMap.build();
  }
}
