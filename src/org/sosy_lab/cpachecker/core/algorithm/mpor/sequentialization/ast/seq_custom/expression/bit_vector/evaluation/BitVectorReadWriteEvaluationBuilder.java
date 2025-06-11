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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

// TODO split this class into access and read-write builders for better overview
public class BitVectorReadWriteEvaluationBuilder {

  public static BitVectorEvaluationExpression buildReadWriteBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.bitVectorEvaluationPrune) {
      return buildPrunedReadWriteBitVectorEvaluationByEncoding(
          pOptions,
          pActiveThread,
          pBitVectorAssignments,
          pDirectReadVariables,
          pDirectWriteVariables,
          pBitVectorVariables,
          pBinaryExpressionBuilder);
    } else {
      return buildFullReadWriteBitVectorEvaluationByEncoding(
          pOptions,
          pActiveThread,
          pDirectReadVariables,
          pDirectWriteVariables,
          pBitVectorVariables,
          pBinaryExpressionBuilder);
    }
  }

  /**
   * Builds a pruned evaluation expression for the given bit vectors based on the variables assigned
   * to the bit vectors in {@code pBitVectorAssignments}.
   */
  private static BitVectorEvaluationExpression buildPrunedReadWriteBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot prune for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildPrunedDenseReadWriteBitVectorEvaluation(
              pActiveThread,
              pDirectReadVariables,
              pDirectWriteVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
      case SCALAR ->
          buildPrunedScalarReadWriteBitVectorEvaluation(
              pActiveThread, pBitVectorVariables, pBitVectorAssignments);
    };
  }

  private static BitVectorEvaluationExpression buildPrunedDenseReadWriteBitVectorEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    Optional<SeqExpression> leftHandSide =
        getPrunedDenseReadWriteBitVectorLeftHandSideEvaluation(
            pActiveThread, pDirectReadVariables, pBitVectorVariables, pBinaryExpressionBuilder);
    Optional<SeqExpression> rightHandSide =
        getPrunedDenseReadWriteBitVectorRightHandSideEvaluation(
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

  private static Optional<SeqExpression> getPrunedDenseReadWriteBitVectorLeftHandSideEvaluation(
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
          buildDenseReadWriteBitVectorLeftHandSideEvaluation(
              directReadBitVector, otherWriteBitVectors, pBinaryExpressionBuilder);
      return Optional.of(new SeqLogicalNotExpression(leftHandSide));
    }
  }

  private static Optional<SeqExpression> getPrunedDenseReadWriteBitVectorRightHandSideEvaluation(
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
          buildDenseReadWriteBitVectorRightHandSideEvaluation(
              directWriteBitVector,
              otherReadBitVectors,
              otherWriteBitVectors,
              pBinaryExpressionBuilder);
      return Optional.of(new SeqLogicalNotExpression(rRightHandSide));
    }
  }

  private static BitVectorEvaluationExpression buildPrunedScalarReadWriteBitVectorEvaluation(
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments) {

    if (pBitVectorVariables.areScalarBitVectorsEmpty()) {
      // no bit vectors (e.g. no global variables) -> no evaluation
      return BitVectorEvaluationExpression.empty();
    }
    // TODO must use direct variable accesses instead of zero assignments here.
    //  the zero assignments are not false per se, just less efficient because the direct variable
    //  accesses are a subset of the 1 assignments
    ImmutableSet<CExpression> zeroAssignments =
        BitVectorUtil.getZeroBitVectorAssignments(pBitVectorAssignments);

    ImmutableList.Builder<SeqExpression> variableExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.scalarReadBitVectors.orElseThrow().entrySet()) {
      CVariableDeclaration variableDeclaration = entry.getKey();

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
                  pBitVectorVariables.scalarWriteBitVectors.orElseThrow().get(variableDeclaration))
              .variables;
      CIdExpression activeWriteVariable =
          BitVectorEvaluationUtil.extractActiveVariable(pActiveThread, writeVariables);
      ImmutableList<SeqExpression> otherWriteVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              activeWriteVariable, writeVariables);

      Optional<SeqLogicalExpression> leftHandSide =
          getPrunedScalarReadWriteBitVectorLeftHandSideEvaluation(
              zeroAssignments, activeReadVariable, otherWriteVariables);
      Optional<SeqLogicalExpression> rightHandSide =
          getPrunedScalarReadWriteBitVectorRightHandSideEvaluation(
              zeroAssignments, activeWriteVariable, otherReadVariables, otherWriteVariables);

      // only add expression if it was not pruned entirely (LHS or RHS present)
      if (leftHandSide.isPresent() || rightHandSide.isPresent()) {
        variableExpressions.add(
            buildPrunedSingleVariableScalarReadWriteBitVectorEvaluation(
                leftHandSide, rightHandSide));
      }
    }
    if (variableExpressions.build().isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    SeqExpression evaluationExpression =
        BitVectorEvaluationUtil.logicalConjunction(variableExpressions.build());
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(evaluationExpression));
  }

  private static Optional<SeqLogicalExpression>
      getPrunedScalarReadWriteBitVectorLeftHandSideEvaluation(
          ImmutableSet<CExpression> pZeroAssignments,
          CIdExpression pActiveReadVariable,
          ImmutableList<SeqExpression> pOtherWriteVariables) {

    if (pZeroAssignments.contains(pActiveReadVariable)) {
      // if the LHS (activeReadVariable) is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    } else {
      // otherwise the LHS is 1, and we only need the right side of the && expression
      return Optional.of(
          buildPrunedSingleVariableScalarReadWriteBitVectorLeftHandSideEvaluation(
              pOtherWriteVariables));
    }
  }

  private static Optional<SeqLogicalExpression>
      getPrunedScalarReadWriteBitVectorRightHandSideEvaluation(
          ImmutableSet<CExpression> pZeroAssignments,
          CIdExpression pActiveWriteVariable,
          ImmutableList<SeqExpression> pOtherReadVariables,
          ImmutableList<SeqExpression> pOtherWriteVariables) {

    if (pZeroAssignments.contains(pActiveWriteVariable)) {
      // if the LHS (activeWriteVariable) is 0, then the entire && expression is 0 -> prune
      return Optional.empty();
    } else {
      // otherwise the LHS is 1, and we only need the right side of the && expression
      return Optional.of(
          buildPrunedSingleVariableScalarReadWriteBitVectorRightHandSideEvaluation(
              pOtherReadVariables, pOtherWriteVariables));
    }
  }

  /**
   * Builds a generalized evaluation expression for the given bit vectors, based on {@code
   * pActiveThread}. This expression does not factor in which values where just assigned to the bit
   * vectors.
   */
  public static BitVectorEvaluationExpression buildFullReadWriteBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY, DECIMAL, HEXADECIMAL -> {
        ImmutableSet<CExpression> otherReadBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                BitVectorAccessType.READ, pActiveThread);
        ImmutableSet<CExpression> otherWriteBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                BitVectorAccessType.WRITE, pActiveThread);
        SeqLogicalAndExpression evaluationExpression =
            buildFullDenseReadWriteBitVectorEvaluation(
                pDirectReadVariables,
                pDirectWriteVariables,
                otherReadBitVectors,
                otherWriteBitVectors,
                pBitVectorVariables,
                pBinaryExpressionBuilder);
        yield new BitVectorEvaluationExpression(
            Optional.empty(), Optional.of(evaluationExpression));
      }
      case SCALAR -> buildScalarReadWriteBitVectorEvaluation(pActiveThread, pBitVectorVariables);
    };
  }

  private static SeqLogicalAndExpression buildFullDenseReadWriteBitVectorEvaluation(
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      ImmutableSet<CExpression> pOtherReadBitVectors,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pOtherReadBitVectors.isEmpty() && !pOtherWriteBitVectors.isEmpty());

    // (R & (W' | W'' | ...))
    CIntegerLiteralExpression directReadBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.globalVariableIds, pDirectReadVariables);
    CExpression leftHandSide =
        buildDenseReadWriteBitVectorLeftHandSideEvaluation(
            directReadBitVector, pOtherWriteBitVectors, pBinaryExpressionBuilder);
    // (W & (R' | R'' | ... | W' | W''))
    CIntegerLiteralExpression directWriteBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.globalVariableIds, pDirectWriteVariables);
    CExpression rightHandSide =
        buildDenseReadWriteBitVectorRightHandSideEvaluation(
            directWriteBitVector,
            pOtherReadBitVectors,
            pOtherWriteBitVectors,
            pBinaryExpressionBuilder);

    // this can also be a binary and & instead of logical and &&, but CBinaryExpression cannot take
    // our SeqLogicalNotExpression as operands and here they are equivalent since we work with 0/1
    return new SeqLogicalAndExpression(
        // we negate with !, not the bit-wise negation ~
        new SeqLogicalNotExpression(leftHandSide), new SeqLogicalNotExpression(rightHandSide));
  }

  private static CBinaryExpression buildDenseReadWriteBitVectorLeftHandSideEvaluation(
      CIntegerLiteralExpression pDirectReadBitVector,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CExpression otherWrites =
        BitVectorEvaluationUtil.binaryDisjunction(pOtherWriteBitVectors, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pDirectReadBitVector, otherWrites, BinaryOperator.BINARY_AND);
  }

  private static CBinaryExpression buildDenseReadWriteBitVectorRightHandSideEvaluation(
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

  private static BitVectorEvaluationExpression buildScalarReadWriteBitVectorEvaluation(
      MPORThread pActiveThread, BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.scalarReadBitVectors.isEmpty()
        && pBitVectorVariables.scalarWriteBitVectors.isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<SeqExpression> variableExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.scalarReadBitVectors.orElseThrow().entrySet()) {
      CVariableDeclaration variableDeclaration = entry.getKey();

      // handle read variables
      ImmutableMap<MPORThread, CIdExpression> readVariables = entry.getValue().variables;
      CIdExpression activeReadVariable =
          BitVectorEvaluationUtil.extractActiveVariable(pActiveThread, readVariables);
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherReadVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              activeReadVariable, readVariables);

      // handle write variables
      ImmutableMap<MPORThread, CIdExpression> writeVariables =
          Objects.requireNonNull(
                  pBitVectorVariables.scalarWriteBitVectors.orElseThrow().get(variableDeclaration))
              .variables;
      CIdExpression activeWriteVariable =
          BitVectorEvaluationUtil.extractActiveVariable(pActiveThread, writeVariables);
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherWriteVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              activeWriteVariable, writeVariables);

      variableExpressions.add(
          buildSingleVariableScalarReadWriteBitVectorEvaluation(
              activeReadVariable, otherReadVariables, activeWriteVariable, otherWriteVariables));
    }
    SeqExpression evaluationExpression =
        BitVectorEvaluationUtil.logicalConjunction(variableExpressions.build());
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(evaluationExpression));
  }

  private static SeqLogicalNotExpression
      buildSingleVariableScalarReadWriteBitVectorLeftHandSideEvaluation(
          CIdExpression pActiveReadVariable, ImmutableList<SeqExpression> pOtherWriteVariables) {

    CToSeqExpression activeReadVariable = new CToSeqExpression(pActiveReadVariable);
    SeqLogicalAndExpression andExpression =
        new SeqLogicalAndExpression(
            activeReadVariable, BitVectorEvaluationUtil.logicalDisjunction(pOtherWriteVariables));
    return new SeqLogicalNotExpression(andExpression);
  }

  private static SeqLogicalNotExpression
      buildPrunedSingleVariableScalarReadWriteBitVectorLeftHandSideEvaluation(
          ImmutableList<SeqExpression> pOtherWriteVariables) {

    return new SeqLogicalNotExpression(
        BitVectorEvaluationUtil.logicalDisjunction(pOtherWriteVariables));
  }

  private static SeqLogicalNotExpression
      buildSingleVariableScalarReadWriteBitVectorRightHandSideEvaluation(
          CIdExpression pActiveWriteVariable,
          ImmutableList<SeqExpression> pOtherReadAndWriteVariables) {

    CToSeqExpression activeWriteVariable = new CToSeqExpression(pActiveWriteVariable);
    SeqLogicalAndExpression andExpression =
        new SeqLogicalAndExpression(
            activeWriteVariable,
            BitVectorEvaluationUtil.logicalDisjunction(pOtherReadAndWriteVariables));
    return new SeqLogicalNotExpression(andExpression);
  }

  private static SeqLogicalNotExpression
      buildPrunedSingleVariableScalarReadWriteBitVectorRightHandSideEvaluation(
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

  private static SeqLogicalAndExpression buildSingleVariableScalarReadWriteBitVectorEvaluation(
      CIdExpression pActiveReadVariable,
      ImmutableList<SeqExpression> pOtherReadVariables,
      CIdExpression pActiveWriteVariable,
      ImmutableList<SeqExpression> pOtherWriteVariables) {

    SeqLogicalNotExpression leftHandSide =
        buildSingleVariableScalarReadWriteBitVectorLeftHandSideEvaluation(
            pActiveReadVariable, pOtherWriteVariables);
    ImmutableList<SeqExpression> otherReadAndWriteVariables =
        ImmutableList.<SeqExpression>builder()
            .addAll(pOtherReadVariables)
            .addAll(pOtherWriteVariables)
            .build();
    SeqLogicalNotExpression rightHandSide =
        buildSingleVariableScalarReadWriteBitVectorRightHandSideEvaluation(
            pActiveWriteVariable, otherReadAndWriteVariables);
    return new SeqLogicalAndExpression(leftHandSide, rightHandSide);
  }

  private static SeqLogicalExpression buildPrunedSingleVariableScalarReadWriteBitVectorEvaluation(
      Optional<SeqLogicalExpression> pLeftHandSide, Optional<SeqLogicalExpression> pRightHandSide) {

    if (pLeftHandSide.isPresent() && pRightHandSide.isEmpty()) {
      return pLeftHandSide.orElseThrow(); // only LHS
    }
    if (pLeftHandSide.isEmpty() && pRightHandSide.isPresent()) {
      return pRightHandSide.orElseThrow(); // only RHS
    }
    return new SeqLogicalAndExpression(pLeftHandSide.orElseThrow(), pRightHandSide.orElseThrow());
  }
}
