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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.ScalarBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorReadWriteEvaluationBuilder {

  static BitVectorEvaluationExpression buildEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
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
        if (pOptions.bitVectorEvaluationPrune) {
          yield buildPrunedDenseEvaluation(
              pActiveThread,
              pDirectReadVariables,
              pDirectWriteVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
        } else {
          yield buildFullDenseEvaluation(
              pActiveThread,
              pDirectReadVariables,
              pDirectWriteVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
        }
      }
      case SCALAR -> {
        if (pOptions.bitVectorEvaluationPrune) {
          yield buildPrunedScalarEvaluation(
              pActiveThread, pDirectReadVariables, pDirectWriteVariables, pBitVectorVariables);
        } else {
          yield buildFullScalarEvaluation(
              pActiveThread, pDirectReadVariables, pDirectWriteVariables, pBitVectorVariables);
        }
      }
    };
  }

  // Pruned Dense Evaluation =======================================================================

  private static BitVectorEvaluationExpression buildPrunedDenseEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    Optional<SeqExpression> leftHandSide =
        buildPrunedDenseLeftHandSide(
            pActiveThread, pDirectReadVariables, pBitVectorVariables, pBinaryExpressionBuilder);
    Optional<SeqExpression> rightHandSide =
        buildPrunedDenseRightHandSide(
            pActiveThread, pDirectWriteVariables, pBitVectorVariables, pBinaryExpressionBuilder);

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
      MPORThread pActiveThread,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectReadVariables.isEmpty()) {
      return Optional.empty();
    } else {
      CIntegerLiteralExpression directReadBitVector =
          BitVectorUtil.buildDirectBitVectorExpression(
              pBitVectorVariables.globalVariableIds, pDirectReadVariables);
      ImmutableSet<CExpression> otherWriteBitVectors =
          pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
              BitVectorAccessType.WRITE, pActiveThread);
      CBinaryExpression leftHandSide =
          buildGeneralDenseLeftHandSide(
              directReadBitVector, otherWriteBitVectors, pBinaryExpressionBuilder);
      return Optional.of(new SeqLogicalNotExpression(leftHandSide));
    }
  }

  private static Optional<SeqExpression> buildPrunedDenseRightHandSide(
      MPORThread pActiveThread,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pDirectWriteVariables.isEmpty()) {
      return Optional.empty();
    } else {
      CIntegerLiteralExpression directWriteBitVector =
          BitVectorUtil.buildDirectBitVectorExpression(
              pBitVectorVariables.globalVariableIds, pDirectWriteVariables);
      ImmutableSet<CExpression> otherReadBitVectors =
          pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
              BitVectorAccessType.READ, pActiveThread);
      ImmutableSet<CExpression> otherWriteBitVectors =
          pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
              BitVectorAccessType.WRITE, pActiveThread);
      CBinaryExpression rRightHandSide =
          buildGeneralDenseRightHandSide(
              directWriteBitVector,
              otherReadBitVectors,
              otherWriteBitVectors,
              pBinaryExpressionBuilder);
      return Optional.of(new SeqLogicalNotExpression(rRightHandSide));
    }
  }

  // Full Dense Evaluation =========================================================================

  private static BitVectorEvaluationExpression buildFullDenseEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableSet<CExpression> otherReadBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            BitVectorAccessType.READ, pActiveThread);
    ImmutableSet<CExpression> otherWriteBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            BitVectorAccessType.WRITE, pActiveThread);

    // (R & (W' | W'' | ...))
    CIntegerLiteralExpression directReadBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.globalVariableIds, pDirectReadVariables);
    CExpression leftHandSide =
        buildGeneralDenseLeftHandSide(
            directReadBitVector, otherWriteBitVectors, pBinaryExpressionBuilder);
    // (W & (R' | R'' | ... | W' | W''))
    CIntegerLiteralExpression directWriteBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.globalVariableIds, pDirectWriteVariables);
    CExpression rightHandSide =
        buildGeneralDenseRightHandSide(
            directWriteBitVector,
            otherReadBitVectors,
            otherWriteBitVectors,
            pBinaryExpressionBuilder);

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
      ImmutableSet<CExpression> pOtherReads,
      ImmutableSet<CExpression> pOtherWrites,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableSet<CExpression> otherReadAndWriteBitVectors =
        ImmutableSet.<CExpression>builder().addAll(pOtherReads).addAll(pOtherWrites).build();
    CExpression otherReadsAndWrites =
        BitVectorEvaluationUtil.binaryDisjunction(
            otherReadAndWriteBitVectors, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pDirectWriteBitVector, otherReadsAndWrites, BinaryOperator.BINARY_AND);
  }

  // Pruned Scalar Evaluation ======================================================================

  private static BitVectorEvaluationExpression buildPrunedScalarEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.areScalarBitVectorsEmpty()) {
      // no bit vectors (e.g. no global variables) -> no evaluation
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<SeqExpression> scalarExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.scalarReadBitVectors.orElseThrow().entrySet()) {
      CVariableDeclaration globalVariable = entry.getKey();

      // handle read variables
      ImmutableMap<MPORThread, CIdExpression> readVariables = entry.getValue().variables;
      CIdExpression activeReadVariable =
          BitVectorEvaluationUtil.extractActiveVariable(pActiveThread, readVariables);
      ImmutableList<SeqExpression> otherReadVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              activeReadVariable, readVariables);

      // handle write variables
      ImmutableMap<MPORThread, CIdExpression> writeVariables =
          Objects.requireNonNull(
                  pBitVectorVariables.scalarWriteBitVectors.orElseThrow().get(globalVariable))
              .variables;
      CIdExpression activeWriteVariable =
          BitVectorEvaluationUtil.extractActiveVariable(pActiveThread, writeVariables);
      ImmutableList<SeqExpression> otherWriteVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              activeWriteVariable, writeVariables);

      Optional<SeqLogicalExpression> leftHandSide =
          buildPrunedScalarLeftHandSide(pDirectReadVariables, globalVariable, otherWriteVariables);
      Optional<SeqLogicalExpression> rightHandSide =
          buildPrunedScalarRightHandSide(
              pDirectWriteVariables, globalVariable, otherReadVariables, otherWriteVariables);

      // only add expression if it was not pruned entirely (LHS or RHS present)
      if (leftHandSide.isPresent() || rightHandSide.isPresent()) {
        scalarExpressions.add(
            buildPrunedScalarSingleVariableEvaluation(leftHandSide, rightHandSide));
      }
    }
    if (scalarExpressions.build().isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    return BitVectorEvaluationUtil.buildScalarLogicalConjunction(scalarExpressions.build());
  }

  /** Builds the logical LHS i.e. {@code !(R && (W' || W'' || ...)}. */
  private static Optional<SeqLogicalExpression> buildPrunedScalarLeftHandSide(
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      CVariableDeclaration pGlobalVariable,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    if (!pDirectReadVariables.contains(pGlobalVariable)) {
      // if the LHS is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    } else {
      // otherwise the LHS is 1, and we only need the right side of the && expression
      return Optional.of(buildPrunedScalarSingleVariableLeftHandSide(pOtherWriteVariables));
    }
  }

  /** Builds the logical RHS i.e. {@code !(W && (R' || R'' || ... || W' || W'' || ...)}. */
  private static Optional<SeqLogicalExpression> buildPrunedScalarRightHandSide(
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
          buildPrunedScalarSingleVariableRightHandSide(pOtherReadVariables, pOtherWriteVariables));
    }
  }

  // Full Scalar Evaluation =======================================================================

  private static BitVectorEvaluationExpression buildFullScalarEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.scalarReadBitVectors.isEmpty()
        && pBitVectorVariables.scalarWriteBitVectors.isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<SeqExpression> scalarExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.scalarReadBitVectors.orElseThrow().entrySet()) {
      CVariableDeclaration globalVariable = entry.getKey();

      // handle read variables
      ImmutableMap<MPORThread, CIdExpression> readVariables = entry.getValue().variables;
      CIdExpression activeReadVariable =
          BitVectorEvaluationUtil.extractActiveVariable(pActiveThread, readVariables);
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherReadVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              activeReadVariable, readVariables);

      // handle write variables
      ScalarBitVector scalarBitVector =
          pBitVectorVariables.scalarWriteBitVectors.orElseThrow().get(globalVariable);
      ImmutableMap<MPORThread, CIdExpression> writeVariables =
          Objects.requireNonNull(scalarBitVector).variables;
      CIdExpression activeWriteVariable =
          BitVectorEvaluationUtil.extractActiveVariable(pActiveThread, writeVariables);
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherWriteVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              activeWriteVariable, writeVariables);

      scalarExpressions.add(
          buildFullScalarSingleVariableEvaluation(
              globalVariable,
              pDirectReadVariables,
              pDirectWriteVariables,
              otherReadVariables,
              otherWriteVariables));
    }
    return BitVectorEvaluationUtil.buildScalarLogicalConjunction(scalarExpressions.build());
  }

  // Pruned Scalar Single Variable Evaluation ======================================================

  private static SeqLogicalNotExpression buildPrunedScalarSingleVariableLeftHandSide(
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    return new SeqLogicalNotExpression(
        BitVectorEvaluationUtil.logicalDisjunction(pOtherWriteVariables));
  }

  private static SeqLogicalNotExpression buildPrunedScalarSingleVariableRightHandSide(
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

  private static SeqLogicalExpression buildPrunedScalarSingleVariableEvaluation(
      Optional<SeqLogicalExpression> pLeftHandSide, Optional<SeqLogicalExpression> pRightHandSide) {

    if (pLeftHandSide.isPresent() && pRightHandSide.isEmpty()) {
      return pLeftHandSide.orElseThrow(); // only LHS
    }
    if (pLeftHandSide.isEmpty() && pRightHandSide.isPresent()) {
      return pRightHandSide.orElseThrow(); // only RHS
    }
    return new SeqLogicalAndExpression(pLeftHandSide.orElseThrow(), pRightHandSide.orElseThrow());
  }

  // Full Scalar Single Variable Evaluation ========================================================

  private static SeqLogicalNotExpression buildFullScalarSingleVariableLeftHandSide(
      CVariableDeclaration pGlobalVariable,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    SeqExpression activeReadValue =
        BitVectorEvaluationUtil.buildScalarDirectBitVector(pGlobalVariable, pDirectReadVariables);
    SeqLogicalAndExpression andExpression =
        new SeqLogicalAndExpression(
            activeReadValue, BitVectorEvaluationUtil.logicalDisjunction(pOtherWriteVariables));
    return new SeqLogicalNotExpression(andExpression);
  }

  private static SeqLogicalNotExpression buildFullScalarSingleVariableRightHandSide(
      CVariableDeclaration pGlobalVariable,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      ImmutableList<SeqExpression> pOtherReadAndWriteVariables) {

    SeqExpression activeWriteValue =
        BitVectorEvaluationUtil.buildScalarDirectBitVector(pGlobalVariable, pDirectWriteVariables);
    SeqLogicalAndExpression andExpression =
        new SeqLogicalAndExpression(
            activeWriteValue,
            BitVectorEvaluationUtil.logicalDisjunction(pOtherReadAndWriteVariables));
    return new SeqLogicalNotExpression(andExpression);
  }

  private static SeqLogicalAndExpression buildFullScalarSingleVariableEvaluation(
      CVariableDeclaration pGlobalVariable,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      ImmutableList<SeqExpression> pOtherReadVariables,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    SeqLogicalNotExpression leftHandSide =
        buildFullScalarSingleVariableLeftHandSide(
            pGlobalVariable, pDirectReadVariables, pOtherWriteVariables);
    ImmutableList<SeqExpression> otherReadAndWriteVariables =
        ImmutableList.<SeqExpression>builder()
            .addAll(pOtherReadVariables)
            .addAll(pOtherWriteVariables)
            .build();
    SeqLogicalNotExpression rightHandSide =
        buildFullScalarSingleVariableRightHandSide(
            pGlobalVariable, pDirectWriteVariables, otherReadAndWriteVariables);
    return new SeqLogicalAndExpression(leftHandSide, rightHandSide);
  }
}
