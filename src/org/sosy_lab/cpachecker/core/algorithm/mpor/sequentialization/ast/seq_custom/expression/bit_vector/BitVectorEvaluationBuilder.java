// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.ScalarBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorEvaluationBuilder {

  // Access Bit Vector Reduction ===================================================================

  /**
   * Builds a pruned evaluation expression for the given bit vectors based on the variables assigned
   * to the bit vectors in {@code pBitVectorAssignments}.
   */
  public static BitVectorEvaluationExpression buildPrunedAccessBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<CVariableDeclaration> pDirectVariables,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      // for access bin/hex, the bit vector evaluation is either full or pruned entirely
      case NONE ->
          throw new IllegalArgumentException(
              "cannot prune for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildPrunedDenseAccessBitVectorEvaluation(
              pActiveThread, pDirectVariables, pBitVectorVariables, pBinaryExpressionBuilder);
      case SCALAR -> {
        Optional<SeqExpression> seqExpression =
            buildPrunedScalarAccessBitVectorEvaluation(
                pActiveThread, pBitVectorVariables, pBitVectorAssignments);
        yield new BitVectorEvaluationExpression(Optional.empty(), seqExpression);
      }
    };
  }

  private static BitVectorEvaluationExpression buildPrunedDenseAccessBitVectorEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<CVariableDeclaration> pDirectVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    assert !pDirectVariables.isEmpty() : "target statements contains no global accesses";

    CIntegerLiteralExpression directBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.globalVariableIds, pDirectVariables);
    ImmutableSet<CExpression> otherBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            BitVectorAccessType.ACCESS, pActiveThread);
    CBinaryExpression binaryExpression =
        buildAccessBitVectorEvaluation(directBitVector, otherBitVectors, pBinaryExpressionBuilder);
    return new BitVectorEvaluationExpression(Optional.of(binaryExpression), Optional.empty());
  }

  private static Optional<SeqExpression> buildPrunedScalarAccessBitVectorEvaluation(
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments) {

    if (pBitVectorVariables.scalarAccessBitVectors.isEmpty()) {
      return Optional.empty();
    }
    ImmutableSet<CExpression> zeroes =
        BitVectorUtil.getZeroBitVectorAssignments(pBitVectorAssignments);
    ImmutableList.Builder<SeqExpression> variableExpressions = ImmutableList.builder();
    ImmutableMap<CVariableDeclaration, ScalarBitVector> scalarBitVectors =
        pBitVectorVariables.getScalarBitVectorsByAccessType(BitVectorAccessType.ACCESS);
    for (var entry : scalarBitVectors.entrySet()) {
      ImmutableMap<MPORThread, CIdExpression> accessVariables = entry.getValue().variables;
      CIdExpression activeVariable = extractActiveVariable(pActiveThread, accessVariables);
      // if the LHS (activeVariable) is 0, then the entire && expression is 0 -> prune
      if (!zeroes.contains(activeVariable)) {
        // convert from CExpression to SeqExpression
        ImmutableList<SeqExpression> otherVariables =
            convertOtherVariablesToSeqExpression(activeVariable, accessVariables);
        SeqLogicalAndExpression andExpression =
            distributeConjunction(activeVariable, otherVariables);
        variableExpressions.add(andExpression);
      }
    }
    if (variableExpressions.build().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(nestLogicalExpressions(variableExpressions.build(), SeqLogicalOperator.OR));
  }

  // TODO
  @SuppressWarnings("unused")
  public static Optional<BitVectorEvaluationExpression> buildBitVectorAccessEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY, DECIMAL, HEXADECIMAL -> {
        CExpression directBitVector =
            pBitVectorVariables.getDenseBitVectorByAccessType(
                BitVectorAccessType.ACCESS, pActiveThread);
        ImmutableSet<CExpression> otherBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                BitVectorAccessType.ACCESS, pActiveThread);
        CBinaryExpression binaryExpression =
            buildAccessBitVectorEvaluation(
                directBitVector, otherBitVectors, pBinaryExpressionBuilder);
        yield Optional.of(
            new BitVectorEvaluationExpression(Optional.of(binaryExpression), Optional.empty()));
      }
      case SCALAR -> {
        Optional<SeqExpression> seqExpression =
            buildScalarAccessBitVectorEvaluation(pActiveThread, pBitVectorVariables);
        yield Optional.of(new BitVectorEvaluationExpression(Optional.empty(), seqExpression));
      }
    };
  }

  private static CBinaryExpression buildAccessBitVectorEvaluation(
      CExpression pDirectBitVector,
      // TODO make list
      ImmutableSet<CExpression> pOtherBitVectors,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pOtherBitVectors.isEmpty());
    checkArgument(!pOtherBitVectors.contains(pDirectBitVector));

    CExpression rightHandSide =
        nestBinaryExpressions(pOtherBitVectors, BinaryOperator.BINARY_OR, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pDirectBitVector, rightHandSide, BinaryOperator.BINARY_AND);
  }

  private static Optional<SeqExpression> buildScalarAccessBitVectorEvaluation(
      MPORThread pActiveThread, BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.scalarAccessBitVectors.isEmpty()) {
      return Optional.empty();
    }
    ImmutableList.Builder<SeqExpression> variableExpressions = ImmutableList.builder();
    ImmutableMap<CVariableDeclaration, ScalarBitVector> scalarBitVectors =
        pBitVectorVariables.getScalarBitVectorsByAccessType(BitVectorAccessType.ACCESS);
    for (var entry : scalarBitVectors.entrySet()) {
      ImmutableMap<MPORThread, CIdExpression> accessVariables = entry.getValue().variables;
      CIdExpression activeVariable = extractActiveVariable(pActiveThread, accessVariables);
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherVariables =
          convertOtherVariablesToSeqExpression(activeVariable, accessVariables);
      SeqLogicalAndExpression andExpression = distributeConjunction(activeVariable, otherVariables);
      variableExpressions.add(andExpression);
    }
    return Optional.of(nestLogicalExpressions(variableExpressions.build(), SeqLogicalOperator.OR));
  }

  // Read/Write Bit Vector Reduction ===============================================================

  /**
   * Builds a pruned evaluation expression for the given bit vectors based on the variables assigned
   * to the bit vectors in {@code pBitVectorAssignments}.
   */
  public static BitVectorEvaluationExpression buildPrunedReadWriteBitVectorEvaluationByEncoding(
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
      case SCALAR -> {
        Optional<SeqExpression> seqExpression =
            buildPrunedScalarReadWriteBitVectorEvaluation(
                pActiveThread, pBitVectorVariables, pBitVectorAssignments);
        yield new BitVectorEvaluationExpression(Optional.empty(), seqExpression);
      }
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
    throw new IllegalArgumentException(
        "both LHS and RHS of read/write bit vector evaluation expression are empty");
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
      CBinaryExpression rLeftHandSide =
          buildDenseReadWriteBitVectorLeftHandSideEvaluation(
              directReadBitVector, otherWriteBitVectors, pBinaryExpressionBuilder);
      return Optional.of(new SeqLogicalNotExpression(rLeftHandSide));
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

  private static Optional<SeqExpression> buildPrunedScalarReadWriteBitVectorEvaluation(
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments) {

    if (pBitVectorVariables.areScalarBitVectorsEmpty()) {
      return Optional.empty(); // no bit vectors (e.g. no global variables) -> no evaluation
    }
    ImmutableSet<CExpression> zeroAssignments =
        BitVectorUtil.getZeroBitVectorAssignments(pBitVectorAssignments);

    ImmutableList.Builder<SeqExpression> variableExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.scalarReadBitVectors.orElseThrow().entrySet()) {
      CVariableDeclaration variableDeclaration = entry.getKey();

      // handle read variables
      ImmutableMap<MPORThread, CIdExpression> readVariables = entry.getValue().variables;
      CIdExpression activeReadVariable = extractActiveVariable(pActiveThread, readVariables);
      ImmutableList<SeqExpression> otherReadVariables =
          convertOtherVariablesToSeqExpression(activeReadVariable, readVariables);

      // handle write variables
      ImmutableMap<MPORThread, CIdExpression> writeVariables =
          Objects.requireNonNull(
                  pBitVectorVariables.scalarWriteBitVectors.orElseThrow().get(variableDeclaration))
              .variables;
      CIdExpression activeWriteVariable = extractActiveVariable(pActiveThread, writeVariables);
      ImmutableList<SeqExpression> otherWriteVariables =
          convertOtherVariablesToSeqExpression(activeWriteVariable, writeVariables);

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
      return Optional.empty();
    }
    return Optional.of(nestLogicalExpressions(variableExpressions.build(), SeqLogicalOperator.AND));
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
  public static Optional<BitVectorEvaluationExpression> buildReadWriteBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.bitVectorEvaluationPrune) {
      return Optional.empty();
    }
    return switch (pOptions.bitVectorEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY, DECIMAL, HEXADECIMAL -> {
        CExpression readBitVector =
            pBitVectorVariables.getDenseBitVectorByAccessType(
                BitVectorAccessType.READ, pActiveThread);
        CExpression writeBitVector =
            pBitVectorVariables.getDenseBitVectorByAccessType(
                BitVectorAccessType.WRITE, pActiveThread);
        ImmutableSet<CExpression> otherReadBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                BitVectorAccessType.READ, pActiveThread);
        ImmutableSet<CExpression> otherWriteBitVectors =
            pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
                BitVectorAccessType.WRITE, pActiveThread);
        SeqLogicalAndExpression evaluationExpression =
            buildDenseReadWriteBitVectorEvaluation(
                readBitVector,
                writeBitVector,
                otherReadBitVectors,
                otherWriteBitVectors,
                pBinaryExpressionBuilder);
        yield Optional.of(
            new BitVectorEvaluationExpression(Optional.empty(), Optional.of(evaluationExpression)));
      }
      case SCALAR -> {
        Optional<SeqExpression> seqExpression =
            buildScalarReadWriteBitVectorEvaluation(pActiveThread, pBitVectorVariables);
        yield Optional.of(new BitVectorEvaluationExpression(Optional.empty(), seqExpression));
      }
    };
  }

  private static SeqLogicalAndExpression buildDenseReadWriteBitVectorEvaluation(
      CExpression pActiveReadBitVector,
      CExpression pActiveWriteBitVector,
      // TODO make list
      ImmutableSet<CExpression> pOtherReadBitVectors,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pOtherReadBitVectors.isEmpty() && !pOtherWriteBitVectors.isEmpty());
    checkArgument(!pOtherReadBitVectors.contains(pActiveReadBitVector));
    checkArgument(!pOtherWriteBitVectors.contains(pActiveWriteBitVector));

    // (R & (W' | W'' | ...))
    CExpression leftHandSide =
        buildDenseReadWriteBitVectorLeftHandSideEvaluation(
            pActiveReadBitVector, pOtherWriteBitVectors, pBinaryExpressionBuilder);
    // (W & (R' | R'' | ... | W' | W''))
    CExpression rightHandSide =
        buildDenseReadWriteBitVectorRightHandSideEvaluation(
            pActiveWriteBitVector,
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
      CExpression pActiveReadBitVector,
      ImmutableSet<CExpression> pOtherWriteBitVectors,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CExpression otherWrites =
        nestBinaryExpressions(
            pOtherWriteBitVectors, BinaryOperator.BINARY_OR, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pActiveReadBitVector, otherWrites, BinaryOperator.BINARY_AND);
  }

  private static CBinaryExpression buildDenseReadWriteBitVectorRightHandSideEvaluation(
      CExpression pActiveWriteBitVector,
      ImmutableSet<CExpression> pOtherReads,
      ImmutableSet<CExpression> pOtherWrites,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableSet<CExpression> otherReadAndWriteBitVectors =
        ImmutableSet.<CExpression>builder().addAll(pOtherReads).addAll(pOtherWrites).build();
    CExpression otherReadsAndWrites =
        nestBinaryExpressions(
            otherReadAndWriteBitVectors, BinaryOperator.BINARY_OR, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pActiveWriteBitVector, otherReadsAndWrites, BinaryOperator.BINARY_AND);
  }

  private static Optional<SeqExpression> buildScalarReadWriteBitVectorEvaluation(
      MPORThread pActiveThread, BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.scalarReadBitVectors.isEmpty()
        && pBitVectorVariables.scalarWriteBitVectors.isEmpty()) {
      return Optional.empty();
    }
    ImmutableList.Builder<SeqExpression> variableExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.scalarReadBitVectors.orElseThrow().entrySet()) {
      CVariableDeclaration variableDeclaration = entry.getKey();

      // handle read variables
      ImmutableMap<MPORThread, CIdExpression> readVariables = entry.getValue().variables;
      CIdExpression activeReadVariable = extractActiveVariable(pActiveThread, readVariables);
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherReadVariables =
          convertOtherVariablesToSeqExpression(activeReadVariable, readVariables);

      // handle write variables
      ImmutableMap<MPORThread, CIdExpression> writeVariables =
          Objects.requireNonNull(
                  pBitVectorVariables.scalarWriteBitVectors.orElseThrow().get(variableDeclaration))
              .variables;
      CIdExpression activeWriteVariable = extractActiveVariable(pActiveThread, writeVariables);
      // convert from CExpression to SeqExpression
      ImmutableList<SeqExpression> otherWriteVariables =
          convertOtherVariablesToSeqExpression(activeWriteVariable, writeVariables);

      variableExpressions.add(
          buildSingleVariableScalarReadWriteBitVectorEvaluation(
              activeReadVariable, otherReadVariables, activeWriteVariable, otherWriteVariables));
    }
    return Optional.of(nestLogicalExpressions(variableExpressions.build(), SeqLogicalOperator.AND));
  }

  private static SeqLogicalNotExpression
      buildSingleVariableScalarReadWriteBitVectorLeftHandSideEvaluation(
          CIdExpression pActiveReadVariable, ImmutableList<SeqExpression> pOtherWriteVariables) {

    CToSeqExpression activeReadVariable = new CToSeqExpression(pActiveReadVariable);
    SeqLogicalAndExpression andExpression =
        new SeqLogicalAndExpression(
            activeReadVariable,
            nestLogicalExpressions(pOtherWriteVariables, SeqLogicalOperator.OR));
    return new SeqLogicalNotExpression(andExpression);
  }

  private static SeqLogicalNotExpression
      buildPrunedSingleVariableScalarReadWriteBitVectorLeftHandSideEvaluation(
          ImmutableList<SeqExpression> pOtherWriteVariables) {

    return new SeqLogicalNotExpression(
        nestLogicalExpressions(pOtherWriteVariables, SeqLogicalOperator.OR));
  }

  private static SeqLogicalNotExpression
      buildSingleVariableScalarReadWriteBitVectorRightHandSideEvaluation(
          CIdExpression pActiveWriteVariable,
          ImmutableList<SeqExpression> pOtherReadAndWriteVariables) {

    CToSeqExpression activeWriteVariable = new CToSeqExpression(pActiveWriteVariable);
    SeqLogicalAndExpression andExpression =
        new SeqLogicalAndExpression(
            activeWriteVariable,
            nestLogicalExpressions(pOtherReadAndWriteVariables, SeqLogicalOperator.OR));
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
        nestLogicalExpressions(otherReadAndWriteVariables, SeqLogicalOperator.OR));
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

  // Binary Expression Helpers =====================================================================

  private static CIdExpression extractActiveVariable(
      MPORThread pActiveThread, ImmutableMap<MPORThread, CIdExpression> pAllVariables) {
    assert pAllVariables.containsKey(pActiveThread) : "no variable found for active thread";
    CIdExpression rActiveVariable = pAllVariables.get(pActiveThread);
    assert rActiveVariable != null;
    return rActiveVariable;
  }

  private static ImmutableList<SeqExpression> convertOtherVariablesToSeqExpression(
      CIdExpression pActiveVariable, ImmutableMap<MPORThread, CIdExpression> pAllVariables) {

    return pAllVariables.values().stream()
        .filter(v -> !v.equals(pActiveVariable))
        .map(CToSeqExpression::new)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns a distributive conjunction expression of the form {@code A && (B || C || ...)} where
   * {@code A} is {@code pSingleTerm} and {@code B, C, ...} given in {@code pDisjunctionTerms}.
   */
  private static SeqLogicalAndExpression distributeConjunction(
      CExpression pSingleTerm, ImmutableList<SeqExpression> pDisjunctionTerms) {

    return distributeConjunction(new CToSeqExpression(pSingleTerm), pDisjunctionTerms);
  }

  /**
   * Returns a distributive conjunction expression of the form {@code A && (B || C || ...)} where
   * {@code A} is {@code pSingleTerm} and {@code B, C, ...} given in {@code pDisjunctionTerms}.
   */
  private static SeqLogicalAndExpression distributeConjunction(
      SeqExpression pSingleTerm, ImmutableList<SeqExpression> pDisjunctionTerms) {

    SeqExpression rightHandSide = nestLogicalExpressions(pDisjunctionTerms, SeqLogicalOperator.OR);
    return new SeqLogicalAndExpression(pSingleTerm, rightHandSide);
  }

  private static CExpression nestBinaryExpressions(
      ImmutableCollection<CExpression> pAllExpressions,
      BinaryOperator pBinaryOperator,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pAllExpressions.isEmpty(), "pAllExpressions must not be empty");

    CExpression rNested = pAllExpressions.iterator().next();
    for (CExpression next : pAllExpressions) {
      if (!next.equals(rNested)) {
        rNested = pBinaryExpressionBuilder.buildBinaryExpression(rNested, next, pBinaryOperator);
      }
    }
    return rNested;
  }

  private static SeqExpression nestLogicalExpressions(
      ImmutableCollection<SeqExpression> pAllExpressions, SeqLogicalOperator pLogicalOperator) {

    checkArgument(!pAllExpressions.isEmpty(), "pAllExpressions must not be empty");

    SeqExpression rNested = pAllExpressions.iterator().next();
    for (SeqExpression next : pAllExpressions) {
      if (!next.equals(rNested)) {
        rNested =
            SeqLogicalExpressionBuilder.buildBinaryLogicalExpressionByOperator(
                pLogicalOperator, rNested, next);
      }
    }
    return rNested;
  }
}
