// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalOrExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorReadWriteEvaluationBuilder {

  static BitVectorEvaluationExpression buildVariableOnlyEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildFullDenseVariableOnlyEvaluation(
              pActiveThread, pOtherThreads, pBitVectorVariables, pBinaryExpressionBuilder);
      case SPARSE ->
          // TODO add support
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
    };
  }

  static BitVectorEvaluationExpression buildEvaluationByEncoding(
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
        // TODO think about dynamic reordering of the LHS/RHS depending on how many variables are
        //  read/written. more variables -> conflict likelier -> put left instead of right. a
        //  conflict on a direct write is also more likelier, since the accesses contain both R/W.
        //  this only works for dense bit vectors.
        if (pOptions.bitVectorEvaluationPrune) {
          yield buildPrunedDenseEvaluation(
              pOtherThreads,
              pDirectReadVariables,
              pDirectWriteVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
        } else {
          yield buildFullDenseEvaluation(
              pOtherThreads,
              pDirectReadVariables,
              pDirectWriteVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
        }
      }
      case SPARSE -> {
        if (pOptions.bitVectorEvaluationPrune) {
          yield buildPrunedSparseEvaluation(
              pOtherThreads, pDirectReadVariables, pDirectWriteVariables, pBitVectorVariables);
        } else {
          yield buildFullSparseEvaluation(
              pOtherThreads, pDirectReadVariables, pDirectWriteVariables, pBitVectorVariables);
        }
      }
    };
  }

  // Pruned Dense Evaluation =======================================================================

  private static BitVectorEvaluationExpression buildPrunedDenseEvaluation(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    Optional<CBinaryExpression> leftHandSide =
        buildPrunedDenseLeftHandSide(
            pOtherThreads, pDirectReadVariables, pBitVectorVariables, pBinaryExpressionBuilder);
    Optional<CBinaryExpression> rightHandSide =
        buildPrunedDenseRightHandSide(
            pOtherThreads, pDirectWriteVariables, pBitVectorVariables, pBinaryExpressionBuilder);

    if (leftHandSide.isPresent() && rightHandSide.isPresent()) {
      // both LHS and RHS present: create or expression: ||
      SeqLogicalOrExpression logicalOr =
          new SeqLogicalOrExpression(leftHandSide.orElseThrow(), rightHandSide.orElseThrow());
      return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(logicalOr));
    } else if (leftHandSide.isPresent()) {
      return new BitVectorEvaluationExpression(leftHandSide, Optional.empty());
    } else if (rightHandSide.isPresent()) {
      return new BitVectorEvaluationExpression(rightHandSide, Optional.empty());
    }
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.empty());
  }

  private static Optional<CBinaryExpression> buildPrunedDenseLeftHandSide(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectReadVariables.isEmpty()) {
      return Optional.empty();
    } else {
      CIntegerLiteralExpression directReadBitVector =
          BitVectorUtil.buildDirectBitVectorExpression(
              pBitVectorVariables.getGlobalVariableIds(), pDirectReadVariables);
      ImmutableSet<CExpression> otherWriteBitVectors =
          pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
              BitVectorAccessType.WRITE, pOtherThreads);
      CBinaryExpression leftHandSide =
          buildGeneralDenseLeftHandSide(
              directReadBitVector, otherWriteBitVectors, pBinaryExpressionBuilder);
      return Optional.of(leftHandSide);
    }
  }

  private static Optional<CBinaryExpression> buildPrunedDenseRightHandSide(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectWriteVariables.isEmpty()) {
      return Optional.empty();
    } else {
      CIntegerLiteralExpression directWriteBitVector =
          BitVectorUtil.buildDirectBitVectorExpression(
              pBitVectorVariables.getGlobalVariableIds(), pDirectWriteVariables);
      ImmutableSet<CExpression> otherAccessBitVectors =
          pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
              BitVectorAccessType.ACCESS, pOtherThreads);
      CBinaryExpression rRightHandSide =
          buildGeneralDenseRightHandSide(
              directWriteBitVector, otherAccessBitVectors, pBinaryExpressionBuilder);
      return Optional.of(rRightHandSide);
    }
  }

  // Full Dense Evaluation =========================================================================

  private static BitVectorEvaluationExpression buildFullDenseEvaluation(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression directReadBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.getGlobalVariableIds(), pDirectReadVariables);
    CIntegerLiteralExpression directWriteBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.getGlobalVariableIds(), pDirectWriteVariables);
    return buildFullDenseLogicalOr(
        directReadBitVector,
        directWriteBitVector,
        pOtherThreads,
        pBitVectorVariables,
        pBinaryExpressionBuilder);
  }

  private static BitVectorEvaluationExpression buildFullDenseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CExpression directReadBitVector =
        pBitVectorVariables.getDenseDirectBitVectorByAccessType(
            BitVectorAccessType.READ, pActiveThread);
    CExpression directWriteBitVector =
        pBitVectorVariables.getDenseDirectBitVectorByAccessType(
            BitVectorAccessType.WRITE, pActiveThread);
    return buildFullDenseLogicalOr(
        directReadBitVector,
        directWriteBitVector,
        pOtherThreads,
        pBitVectorVariables,
        pBinaryExpressionBuilder);
  }

  private static BitVectorEvaluationExpression buildFullDenseLogicalOr(
      CExpression pDirectReadBitVector,
      CExpression pDirectWriteBitVector,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableSet<CExpression> otherWriteBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            BitVectorAccessType.WRITE, pOtherThreads);
    ImmutableSet<CExpression> otherAccessBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            BitVectorAccessType.ACCESS, pOtherThreads);

    // (R & (W' | W'' | ...))
    CExpression leftHandSide =
        buildGeneralDenseLeftHandSide(
            pDirectReadBitVector, otherWriteBitVectors, pBinaryExpressionBuilder);
    // (W & (A' | A'' | ...))
    CExpression rightHandSide =
        buildGeneralDenseRightHandSide(
            pDirectWriteBitVector, otherAccessBitVectors, pBinaryExpressionBuilder);
    // (R & (W' | W'' | ...)) || (W & (A' | A'' | ...))
    SeqLogicalOrExpression logicalOr = new SeqLogicalOrExpression(leftHandSide, rightHandSide);
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(logicalOr));
  }

  // General Dense Evaluation ======================================================================

  /** General = used for both pruned and full evaluations. */
  private static CBinaryExpression buildGeneralDenseLeftHandSide(
      CExpression pDirectReadBitVector,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CExpression otherWrites =
        BitVectorEvaluationUtil.binaryDisjunction(pOtherWriteBitVectors, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pDirectReadBitVector, otherWrites, BinaryOperator.BINARY_AND);
  }

  /** General = used for both pruned and full evaluations. */
  private static CBinaryExpression buildGeneralDenseRightHandSide(
      CExpression pDirectWriteBitVector,
      ImmutableSet<CExpression> pOtherAccesses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CExpression otherReadsAndWrites =
        BitVectorEvaluationUtil.binaryDisjunction(pOtherAccesses, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pDirectWriteBitVector, otherReadsAndWrites, BinaryOperator.BINARY_AND);
  }

  // Pruned Sparse Evaluation ======================================================================

  private static BitVectorEvaluationExpression buildPrunedSparseEvaluation(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.areSparseBitVectorsEmpty()) {
      // no bit vectors (e.g. no global variables) -> no evaluation
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<SeqExpression> sparseExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
      CVariableDeclaration globalVariable = entry.getKey();

      // handle write variables
      ImmutableMap<MPORThread, CIdExpression> writeVariables =
          Objects.requireNonNull(pBitVectorVariables.getSparseWriteBitVectors().get(globalVariable))
              .variables;
      ImmutableList<SeqExpression> otherWriteVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              pOtherThreads, writeVariables);

      // handle access variables
      ImmutableMap<MPORThread, CIdExpression> accessVariables = entry.getValue().variables;
      ImmutableList<SeqExpression> otherAccessVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              pOtherThreads, accessVariables);

      Optional<SeqExpression> leftHandSide =
          buildPrunedSparseLeftHandSide(pDirectReadVariables, globalVariable, otherWriteVariables);
      Optional<SeqExpression> rightHandSide =
          buildPrunedSparseRightHandSide(
              pDirectWriteVariables, globalVariable, otherAccessVariables);

      // only add expression if it was not pruned entirely (LHS or RHS present)
      if (leftHandSide.isPresent() || rightHandSide.isPresent()) {
        sparseExpressions.add(
            buildPrunedSparseSingleVariableEvaluation(leftHandSide, rightHandSide));
      }
    }
    if (sparseExpressions.build().isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    return BitVectorEvaluationUtil.buildSparseLogicalDisjunction(sparseExpressions.build());
  }

  /** Builds the logical LHS i.e. {@code (R && (W' || W'' || ...))}. */
  private static Optional<SeqExpression> buildPrunedSparseLeftHandSide(
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      CVariableDeclaration pGlobalVariable,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    if (!pDirectReadVariables.contains(pGlobalVariable)) {
      // if the LHS is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    } else {
      // otherwise the LHS is 1, and we only need the right side of the && expression
      return Optional.of(BitVectorEvaluationUtil.logicalDisjunction(pOtherWriteVariables));
    }
  }

  /** Builds the logical RHS i.e. {@code (W && (A' || A'' || ...))}. */
  private static Optional<SeqExpression> buildPrunedSparseRightHandSide(
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      CVariableDeclaration pGlobalVariable,
      ImmutableList<SeqExpression> pOtherAccessVariables) {

    if (!pDirectWriteVariables.contains(pGlobalVariable)) {
      // if the LHS (activeWriteVariable) is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    } else {
      // otherwise the LHS is 1, and we only need the right side of the && expression
      return Optional.of(BitVectorEvaluationUtil.logicalDisjunction(pOtherAccessVariables));
    }
  }

  // Full Sparse Evaluation ========================================================================

  private static BitVectorEvaluationExpression buildFullSparseEvaluation(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.areSparseBitVectorsEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<SeqExpression> sparseExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
      CVariableDeclaration globalVariable = entry.getKey();

      // handle write variables
      SparseBitVector sparseBitVector =
          pBitVectorVariables.getSparseWriteBitVectors().get(globalVariable);
      ImmutableMap<MPORThread, CIdExpression> writeVariables =
          Objects.requireNonNull(sparseBitVector).variables;
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherWriteVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              pOtherThreads, writeVariables);

      // handle access variables
      ImmutableMap<MPORThread, CIdExpression> accessVariables = entry.getValue().variables;
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherAccessVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              pOtherThreads, accessVariables);

      sparseExpressions.add(
          buildFullSparseSingleVariableEvaluation(
              globalVariable,
              pDirectReadVariables,
              pDirectWriteVariables,
              otherWriteVariables,
              otherAccessVariables));
    }
    return BitVectorEvaluationUtil.buildSparseLogicalDisjunction(sparseExpressions.build());
  }

  // Pruned Sparse Single Variable Evaluation ======================================================

  private static SeqExpression buildPrunedSparseSingleVariableEvaluation(
      Optional<SeqExpression> pLeftHandSide, Optional<SeqExpression> pRightHandSide) {

    if (pLeftHandSide.isPresent() && pRightHandSide.isEmpty()) {
      return pLeftHandSide.orElseThrow(); // only LHS
    }
    if (pLeftHandSide.isEmpty() && pRightHandSide.isPresent()) {
      return pRightHandSide.orElseThrow(); // only RHS
    }
    return new SeqLogicalOrExpression(pLeftHandSide.orElseThrow(), pRightHandSide.orElseThrow());
  }

  // Full Sparse Single Variable Evaluation ========================================================

  private static SeqLogicalAndExpression buildFullSparseSingleVariableLeftHandSide(
      CVariableDeclaration pGlobalVariable,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    SeqExpression activeReadValue =
        BitVectorEvaluationUtil.buildSparseDirectBitVector(pGlobalVariable, pDirectReadVariables);
    return new SeqLogicalAndExpression(
        activeReadValue, BitVectorEvaluationUtil.logicalDisjunction(pOtherWriteVariables));
  }

  private static SeqLogicalAndExpression buildFullSparseSingleVariableRightHandSide(
      CVariableDeclaration pGlobalVariable,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      ImmutableList<SeqExpression> pOtherReadAndWriteVariables) {

    SeqExpression activeWriteValue =
        BitVectorEvaluationUtil.buildSparseDirectBitVector(pGlobalVariable, pDirectWriteVariables);
    return new SeqLogicalAndExpression(
        activeWriteValue, BitVectorEvaluationUtil.logicalDisjunction(pOtherReadAndWriteVariables));
  }

  private static SeqLogicalOrExpression buildFullSparseSingleVariableEvaluation(
      CVariableDeclaration pGlobalVariable,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      ImmutableList<SeqExpression> pOtherWriteVariables,
      ImmutableList<SeqExpression> pOtherAccessVariables) {

    SeqLogicalAndExpression leftHandSide =
        buildFullSparseSingleVariableLeftHandSide(
            pGlobalVariable, pDirectReadVariables, pOtherWriteVariables);
    SeqLogicalAndExpression rightHandSide =
        buildFullSparseSingleVariableRightHandSide(
            pGlobalVariable, pDirectWriteVariables, pOtherAccessVariables);
    return new SeqLogicalOrExpression(leftHandSide, rightHandSide);
  }
}
