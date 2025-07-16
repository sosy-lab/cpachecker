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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorReadWriteEvaluationBuilder {

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

    Optional<SeqExpression> leftHandSide =
        buildPrunedDenseLeftHandSide(
            pOtherThreads, pDirectReadVariables, pBitVectorVariables, pBinaryExpressionBuilder);
    Optional<SeqExpression> rightHandSide =
        buildPrunedDenseRightHandSide(
            pOtherThreads, pDirectWriteVariables, pBitVectorVariables, pBinaryExpressionBuilder);

    if (leftHandSide.isPresent() && rightHandSide.isPresent()) {
      // both LHS and RHS present: create &&
      SeqLogicalAndExpression logicalAnd =
          new SeqLogicalAndExpression(leftHandSide.orElseThrow(), rightHandSide.orElseThrow());
      return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(logicalAnd));
    } else if (leftHandSide.isPresent()) {
      return new BitVectorEvaluationExpression(Optional.empty(), leftHandSide);
    } else if (rightHandSide.isPresent()) {
      return new BitVectorEvaluationExpression(Optional.empty(), rightHandSide);
    }
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.empty());
  }

  private static Optional<SeqExpression> buildPrunedDenseLeftHandSide(
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
      return Optional.of(new SeqLogicalNotExpression(leftHandSide));
    }
  }

  private static Optional<SeqExpression> buildPrunedDenseRightHandSide(
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
      return Optional.of(new SeqLogicalNotExpression(rRightHandSide));
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

    ImmutableSet<CExpression> otherWriteBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            BitVectorAccessType.WRITE, pOtherThreads);
    ImmutableSet<CExpression> otherAccessBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            BitVectorAccessType.ACCESS, pOtherThreads);

    // (R & (W' | W'' | ...))
    CIntegerLiteralExpression directReadBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.getGlobalVariableIds(), pDirectReadVariables);
    CExpression leftHandSide =
        buildGeneralDenseLeftHandSide(
            directReadBitVector, otherWriteBitVectors, pBinaryExpressionBuilder);
    // (W & (A' | A'' | ...))
    CIntegerLiteralExpression directWriteBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.getGlobalVariableIds(), pDirectWriteVariables);
    CExpression rightHandSide =
        buildGeneralDenseRightHandSide(
            directWriteBitVector, otherAccessBitVectors, pBinaryExpressionBuilder);

    SeqLogicalAndExpression logicalAnd =
        new SeqLogicalAndExpression(
            new SeqLogicalNotExpression(leftHandSide), new SeqLogicalNotExpression(rightHandSide));
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(logicalAnd));
  }

  // General Dense Evaluation ======================================================================

  /** General = used for both pruned and full evaluations. */
  private static CBinaryExpression buildGeneralDenseLeftHandSide(
      CIntegerLiteralExpression pDirectReadBitVector,
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
      CIntegerLiteralExpression pDirectWriteBitVector,
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

      // handle read variables
      ImmutableMap<MPORThread, CIdExpression> readVariables = entry.getValue().variables;
      ImmutableList<SeqExpression> otherReadVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              pOtherThreads, readVariables);

      // handle write variables
      ImmutableMap<MPORThread, CIdExpression> writeVariables =
          Objects.requireNonNull(pBitVectorVariables.getSparseWriteBitVectors().get(globalVariable))
              .variables;
      ImmutableList<SeqExpression> otherWriteVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              pOtherThreads, writeVariables);

      Optional<SeqLogicalExpression> leftHandSide =
          buildPrunedSparseLeftHandSide(pDirectReadVariables, globalVariable, otherWriteVariables);
      Optional<SeqLogicalExpression> rightHandSide =
          buildPrunedSparseRightHandSide(
              pDirectWriteVariables, globalVariable, otherReadVariables, otherWriteVariables);

      // only add expression if it was not pruned entirely (LHS or RHS present)
      if (leftHandSide.isPresent() || rightHandSide.isPresent()) {
        sparseExpressions.add(
            buildPrunedSparseSingleVariableEvaluation(leftHandSide, rightHandSide));
      }
    }
    if (sparseExpressions.build().isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    return BitVectorEvaluationUtil.buildSparseLogicalConjunction(sparseExpressions.build());
  }

  /** Builds the logical LHS i.e. {@code !(R && (W' || W'' || ...)}. */
  private static Optional<SeqLogicalExpression> buildPrunedSparseLeftHandSide(
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      CVariableDeclaration pGlobalVariable,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    if (!pDirectReadVariables.contains(pGlobalVariable)) {
      // if the LHS is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    } else {
      // otherwise the LHS is 1, and we only need the right side of the && expression
      return Optional.of(buildPrunedSparseSingleVariableLeftHandSide(pOtherWriteVariables));
    }
  }

  /** Builds the logical RHS i.e. {@code !(W && (R' || R'' || ... || W' || W'' || ...)}. */
  private static Optional<SeqLogicalExpression> buildPrunedSparseRightHandSide(
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      CVariableDeclaration pGlobalVariable,
      ImmutableList<SeqExpression> pOtherReadVariables,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    if (!pDirectWriteVariables.contains(pGlobalVariable)) {
      // if the LHS (activeWriteVariable) is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    } else {
      // otherwise the LHS is 1, and we only need the right side of the && expression
      return Optional.of(
          buildPrunedSparseSingleVariableRightHandSide(pOtherReadVariables, pOtherWriteVariables));
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

      // handle read variables
      ImmutableMap<MPORThread, CIdExpression> readVariables = entry.getValue().variables;
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherReadVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              pOtherThreads, readVariables);

      // handle write variables
      SparseBitVector sparseBitVector =
          pBitVectorVariables.getSparseWriteBitVectors().get(globalVariable);
      ImmutableMap<MPORThread, CIdExpression> writeVariables =
          Objects.requireNonNull(sparseBitVector).variables;
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherWriteVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              pOtherThreads, writeVariables);

      sparseExpressions.add(
          buildFullSparseSingleVariableEvaluation(
              globalVariable,
              pDirectReadVariables,
              pDirectWriteVariables,
              otherReadVariables,
              otherWriteVariables));
    }
    return BitVectorEvaluationUtil.buildSparseLogicalConjunction(sparseExpressions.build());
  }

  // Pruned Sparse Single Variable Evaluation ======================================================

  private static SeqLogicalNotExpression buildPrunedSparseSingleVariableLeftHandSide(
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    return new SeqLogicalNotExpression(
        BitVectorEvaluationUtil.logicalDisjunction(pOtherWriteVariables));
  }

  private static SeqLogicalNotExpression buildPrunedSparseSingleVariableRightHandSide(
      ImmutableList<SeqExpression> pOtherReadVariables,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    ImmutableList<SeqExpression> otherReadAndWriteVariables =
        ImmutableList.<SeqExpression>builder()
            .addAll(pOtherReadVariables)
            .addAll(pOtherWriteVariables)
            .build();
    return new SeqLogicalNotExpression(
        BitVectorEvaluationUtil.logicalDisjunction(otherReadAndWriteVariables));
  }

  private static SeqLogicalExpression buildPrunedSparseSingleVariableEvaluation(
      Optional<SeqLogicalExpression> pLeftHandSide, Optional<SeqLogicalExpression> pRightHandSide) {

    if (pLeftHandSide.isPresent() && pRightHandSide.isEmpty()) {
      return pLeftHandSide.orElseThrow(); // only LHS
    }
    if (pLeftHandSide.isEmpty() && pRightHandSide.isPresent()) {
      return pRightHandSide.orElseThrow(); // only RHS
    }
    return new SeqLogicalAndExpression(pLeftHandSide.orElseThrow(), pRightHandSide.orElseThrow());
  }

  // Full Sparse Single Variable Evaluation ========================================================

  private static SeqLogicalNotExpression buildFullSparseSingleVariableLeftHandSide(
      CVariableDeclaration pGlobalVariable,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    SeqExpression activeReadValue =
        BitVectorEvaluationUtil.buildSparseDirectBitVector(pGlobalVariable, pDirectReadVariables);
    SeqLogicalAndExpression andExpression =
        new SeqLogicalAndExpression(
            activeReadValue, BitVectorEvaluationUtil.logicalDisjunction(pOtherWriteVariables));
    return new SeqLogicalNotExpression(andExpression);
  }

  private static SeqLogicalNotExpression buildFullSparseSingleVariableRightHandSide(
      CVariableDeclaration pGlobalVariable,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      ImmutableList<SeqExpression> pOtherReadAndWriteVariables) {

    SeqExpression activeWriteValue =
        BitVectorEvaluationUtil.buildSparseDirectBitVector(pGlobalVariable, pDirectWriteVariables);
    SeqLogicalAndExpression andExpression =
        new SeqLogicalAndExpression(
            activeWriteValue,
            BitVectorEvaluationUtil.logicalDisjunction(pOtherReadAndWriteVariables));
    return new SeqLogicalNotExpression(andExpression);
  }

  private static SeqLogicalAndExpression buildFullSparseSingleVariableEvaluation(
      CVariableDeclaration pGlobalVariable,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      ImmutableList<SeqExpression> pOtherReadVariables,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    SeqLogicalNotExpression leftHandSide =
        buildFullSparseSingleVariableLeftHandSide(
            pGlobalVariable, pDirectReadVariables, pOtherWriteVariables);
    ImmutableList<SeqExpression> otherReadAndWriteVariables =
        ImmutableList.<SeqExpression>builder()
            .addAll(pOtherReadVariables)
            .addAll(pOtherWriteVariables)
            .build();
    SeqLogicalNotExpression rightHandSide =
        buildFullSparseSingleVariableRightHandSide(
            pGlobalVariable, pDirectWriteVariables, otherReadAndWriteVariables);
    return new SeqLogicalAndExpression(leftHandSide, rightHandSide);
  }
}
