// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqExpressionBuilder {

  // CArraySubscriptExpression =====================================================================

  public static CArraySubscriptExpression buildPcSubscriptExpression(CExpression pSubscriptExpr) {
    return new CArraySubscriptExpression(
        FileLocation.DUMMY, SeqArrayType.INT_ARRAY, SeqIdExpression.DUMMY_PC, pSubscriptExpr);
  }

  static ImmutableList<CArraySubscriptExpression> buildArrayPcExpressions(int pNumThreads) {
    Builder<CArraySubscriptExpression> rArrayPc = ImmutableList.builder();
    for (int i = 0; i < pNumThreads; i++) {
      rArrayPc.add(buildPcSubscriptExpression(buildIntegerLiteralExpression(i)));
    }
    return rArrayPc.build();
  }

  // CBinaryExpression =============================================================================

  /**
   * Returns {@code pc[pThreadId] != -1} for array and {@code pc{pThreadId} != -1} for scalar {@code
   * pc}.
   */
  public static CBinaryExpression buildPcUnequalExitPc(
      PcVariables pPcVariables, int pThreadId, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return pBinaryExpressionBuilder.buildBinaryExpression(
        pPcVariables.get(pThreadId),
        SeqIntegerLiteralExpression.INT_EXIT_PC,
        BinaryOperator.NOT_EQUALS);
  }

  /** Returns {@code next_thread != pThreadId}. */
  public static CBinaryExpression buildNextThreadUnequal(
      int pThreadId, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return pBinaryExpressionBuilder.buildBinaryExpression(
        SeqIdExpression.NEXT_THREAD,
        buildIntegerLiteralExpression(pThreadId),
        BinaryOperator.NOT_EQUALS);
  }

  // TODO need separate BitVectorEvaluationBuilder
  // Bit Vector Access Reduction ===================================================================

  /**
   * Builds a pruned evaluation expression for the given bit vectors based on the variables assigned
   * to the bit vectors in {@code pBitVectorAssignments}.
   */
  public static BitVectorEvaluationExpression buildPrunedAccessBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments,
      BitVectorVariables pBitVectorVariables,
      Optional<BitVectorEvaluationExpression> pFullEvaluation) {

    if (pFullEvaluation.isPresent()) {
      assert !pOptions.pruneBitVectorEvaluation
          : "full evaluation is present, but pruneBitVectorEvaluation is enabled";
      return pFullEvaluation.orElseThrow();
    }
    return switch (pOptions.porBitVectorEncoding) {
      // we only prune for scalar bit vectors
      case NONE, BINARY, HEXADECIMAL ->
          throw new IllegalArgumentException(
              "cannot prune for encoding " + pOptions.porBitVectorEncoding);
      case SCALAR -> {
        Optional<SeqExpression> seqExpression =
            buildPrunedScalarAccessBitVectorAccessEvaluation(
                pActiveThread, pBitVectorVariables, pBitVectorAssignments);
        yield new BitVectorEvaluationExpression(Optional.empty(), seqExpression);
      }
    };
  }

  private static Optional<SeqExpression> buildPrunedScalarAccessBitVectorAccessEvaluation(
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments) {

    if (pBitVectorVariables.scalarAccessBitVectors.isEmpty()) {
      return Optional.empty();
    }
    ImmutableSet<CExpression> zeroes =
        BitVectorUtil.getZeroesFromBitVectorAssignments(pBitVectorAssignments);
    Builder<SeqExpression> variableExpressions = ImmutableList.builder();
    for (var entry :
        pBitVectorVariables
            .getScalarBitVectorsByAccessType(BitVectorAccessType.ACCESS)
            .entrySet()) {
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

  /**
   * Builds a generalized evaluation expression for the given bit vectors, based on {@code
   * pActiveThread}. This expression does not factor in which values where just assigned to the
   * bitvectors.
   */
  public static Optional<BitVectorEvaluationExpression> buildBitVectorAccessEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.pruneBitVectorEvaluation) {
      return Optional.empty(); // if we prune, we later build evaluations dynamically
    }
    return switch (pOptions.porBitVectorEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY, HEXADECIMAL -> {
        CExpression bitVector =
            pBitVectorVariables.getDenseBitVectorByAccessType(
                BitVectorAccessType.ACCESS, pActiveThread);
        ImmutableSet<CExpression> otherBitVectors =
            pBitVectorVariables.getOtherDenseBitVectorsByAccessType(
                BitVectorAccessType.ACCESS, pActiveThread);
        CBinaryExpression binaryExpression =
            buildBitVectorAccessEvaluation(bitVector, otherBitVectors, pBinaryExpressionBuilder);
        yield Optional.of(
            new BitVectorEvaluationExpression(Optional.of(binaryExpression), Optional.empty()));
      }
      case SCALAR -> {
        Optional<SeqExpression> seqExpression =
            buildScalarBitVectorAccessEvaluation(pActiveThread, pBitVectorVariables);
        yield Optional.of(new BitVectorEvaluationExpression(Optional.empty(), seqExpression));
      }
    };
  }

  private static CBinaryExpression buildBitVectorAccessEvaluation(
      CExpression pActiveBitVector,
      // TODO make list
      ImmutableSet<CExpression> pOtherBitVectors,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pOtherBitVectors.isEmpty());
    checkArgument(!pOtherBitVectors.contains(pActiveBitVector));

    CExpression rightHandSide =
        nestBinaryExpressions(pOtherBitVectors, BinaryOperator.BINARY_OR, pBinaryExpressionBuilder);
    return pBinaryExpressionBuilder.buildBinaryExpression(
        pActiveBitVector, rightHandSide, BinaryOperator.BINARY_AND);
  }

  private static Optional<SeqExpression> buildScalarBitVectorAccessEvaluation(
      MPORThread pActiveThread, BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.scalarAccessBitVectors.isEmpty()) {
      return Optional.empty();
    }
    Builder<SeqExpression> variableExpressions = ImmutableList.builder();
    for (var entry :
        pBitVectorVariables
            .getScalarBitVectorsByAccessType(BitVectorAccessType.ACCESS)
            .entrySet()) {
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

  // Bit Vector Read/Write Reduction ===============================================================

  /**
   * Builds a pruned evaluation expression for the given bit vectors based on the variables assigned
   * to the bit vectors in {@code pBitVectorAssignments}.
   */
  public static BitVectorEvaluationExpression buildPrunedReadWriteBitVectorEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments,
      BitVectorVariables pBitVectorVariables,
      Optional<BitVectorEvaluationExpression> pFullEvaluation) {

    if (pFullEvaluation.isPresent()) {
      assert !pOptions.pruneBitVectorEvaluation
          : "full evaluation is present, but pruneBitVectorEvaluation is enabled";
      return pFullEvaluation.orElseThrow();
    }
    return switch (pOptions.porBitVectorEncoding) {
      // we only prune for scalar bit vectors
      case NONE, BINARY, HEXADECIMAL ->
          throw new IllegalArgumentException(
              "cannot prune for encoding " + pOptions.porBitVectorEncoding);
      case SCALAR -> {
        Optional<SeqExpression> seqExpression =
            buildPrunedScalarReadWriteBitVectorAccessEvaluation(
                pActiveThread, pBitVectorVariables, pBitVectorAssignments);
        yield new BitVectorEvaluationExpression(Optional.empty(), seqExpression);
      }
    };
  }

  private static Optional<SeqExpression> buildPrunedScalarReadWriteBitVectorAccessEvaluation(
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments) {

    if (pBitVectorVariables.scalarReadBitVectors.isEmpty()
        && pBitVectorVariables.scalarWriteBitVectors.isEmpty()) {
      return Optional.empty();
    }
    ImmutableSet<CExpression> zeroes =
        BitVectorUtil.getZeroesFromBitVectorAssignments(pBitVectorAssignments);

    Builder<SeqExpression> variableExpressions = ImmutableList.builder();
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

      Optional<SeqLogicalExpression> rightHandSide = Optional.empty();
      Optional<SeqLogicalExpression> leftHandSide = Optional.empty();

      // if the LHS (activeReadVariable) is 0, then the entire && expression is 0 -> prune
      if (!zeroes.contains(activeReadVariable)) {
        // convert from CExpression to SeqExpression
        leftHandSide =
            Optional.of(
                buildSingleVariableScalarReadWriteBitVectorLeftHandSideEvaluation(
                    activeReadVariable, otherWriteVariables));
      }
      // if the LHS (activeWriteVariable) is 0, then the entire && expression is 0 -> prune
      if (!zeroes.contains(activeWriteVariable)) {
        // convert from CExpression to SeqExpression
        ImmutableList<SeqExpression> otherReadAndWriteVariables =
            ImmutableList.<SeqExpression>builder()
                .addAll(otherReadVariables)
                .addAll(otherWriteVariables)
                .build();
        rightHandSide =
            Optional.of(
                buildSingleVariableScalarReadWriteBitVectorRightHandSideEvaluation(
                    activeWriteVariable, otherReadAndWriteVariables));
      }
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

  /**
   * Builds a generalized evaluation expression for the given bit vectors, based on {@code
   * pActiveThread}. This expression does not factor in which values where just assigned to the bit
   * vectors.
   */
  public static Optional<BitVectorEvaluationExpression> buildBitVectorReadWriteEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.pruneBitVectorEvaluation) {
      return Optional.empty();
    }
    return switch (pOptions.porBitVectorEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY, HEXADECIMAL -> {
        CExpression readBitVector =
            pBitVectorVariables.getDenseBitVectorByAccessType(
                BitVectorAccessType.READ, pActiveThread);
        CExpression writeBitVector =
            pBitVectorVariables.getDenseBitVectorByAccessType(
                BitVectorAccessType.WRITE, pActiveThread);
        ImmutableSet<CExpression> otherReadBitVectors =
            pBitVectorVariables.getOtherDenseBitVectorsByAccessType(
                BitVectorAccessType.READ, pActiveThread);
        ImmutableSet<CExpression> otherWriteBitVectors =
            pBitVectorVariables.getOtherDenseBitVectorsByAccessType(
                BitVectorAccessType.WRITE, pActiveThread);
        SeqLogicalAndExpression evaluationExpression =
            SeqExpressionBuilder.buildBitVectorReadWriteEvaluation(
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

  private static SeqLogicalAndExpression buildBitVectorReadWriteEvaluation(
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

    CExpression otherWrites =
        nestBinaryExpressions(
            pOtherWriteBitVectors, BinaryOperator.BINARY_OR, pBinaryExpressionBuilder);
    ImmutableSet<CExpression> otherReadAndWriteBitVectors =
        ImmutableSet.<CExpression>builder()
            .addAll(pOtherReadBitVectors)
            .addAll(pOtherWriteBitVectors)
            .build();
    CExpression otherReadsAndWrites =
        nestBinaryExpressions(
            otherReadAndWriteBitVectors, BinaryOperator.BINARY_OR, pBinaryExpressionBuilder);
    // (R & (W' | W'' | ...))
    CExpression leftHandSide =
        pBinaryExpressionBuilder.buildBinaryExpression(
            pActiveReadBitVector, otherWrites, BinaryOperator.BINARY_AND);
    // (W & (R' | R'' | ... | W' | W''))
    CExpression rightHandSide =
        pBinaryExpressionBuilder.buildBinaryExpression(
            pActiveWriteBitVector, otherReadsAndWrites, BinaryOperator.BINARY_AND);
    // this can also be a binary and & instead of logical and &&, but CBinaryExpression cannot take
    // our SeqLogicalNotExpression as operands and here they are equivalent since we work with 0/1
    return new SeqLogicalAndExpression(
        // we negate with !, not the bit-wise negation ~
        new SeqLogicalNotExpression(leftHandSide), new SeqLogicalNotExpression(rightHandSide));
  }

  private static Optional<SeqExpression> buildScalarReadWriteBitVectorEvaluation(
      MPORThread pActiveThread, BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.scalarReadBitVectors.isEmpty()
        && pBitVectorVariables.scalarWriteBitVectors.isEmpty()) {
      return Optional.empty();
    }
    Builder<SeqExpression> variableExpressions = ImmutableList.builder();
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

  // CFunctionCallExpression =======================================================================

  /**
   * Returns the {@link CFunctionCallExpression} of {@code reach_error("{pFile}", {pLine},
   * "{pFunction}")}
   */
  public static CFunctionCallExpression buildReachError(String pFile, int pLine, String pFunction) {

    CStringLiteralExpression file =
        buildStringLiteralExpression(SeqStringUtil.wrapInQuotationMarks(pFile));
    CIntegerLiteralExpression line = buildIntegerLiteralExpression(pLine);
    CStringLiteralExpression function =
        buildStringLiteralExpression(SeqStringUtil.wrapInQuotationMarks(pFunction));

    return buildFunctionCallExpression(
        SeqVoidType.VOID,
        SeqIdExpression.REACH_ERROR,
        ImmutableList.of(file, line, function),
        SeqFunctionDeclaration.REACH_ERROR);
  }

  // TODO add function that takes MPOROptions and returns (u)int
  public static CFunctionCallExpression buildVerifierNondetInt() {
    return buildFunctionCallExpression(
        SeqSimpleType.INT,
        SeqIdExpression.VERIFIER_NONDET_INT,
        ImmutableList.of(),
        SeqFunctionDeclaration.VERIFIER_NONDET_INT);
  }

  public static CFunctionCallExpression buildVerifierNondetUint() {
    return buildFunctionCallExpression(
        SeqSimpleType.UNSIGNED_INT,
        SeqIdExpression.VERIFIER_NONDET_UINT,
        ImmutableList.of(),
        SeqFunctionDeclaration.VERIFIER_NONDET_UINT);
  }

  private static CFunctionCallExpression buildFunctionCallExpression(
      CType pType,
      CExpression pFunctionName,
      List<CExpression> pParameters,
      CFunctionDeclaration pDeclaration) {

    return new CFunctionCallExpression(
        FileLocation.DUMMY, pType, pFunctionName, pParameters, pDeclaration);
  }

  // CIntegerLiteralExpression =====================================================================

  public static CIntegerLiteralExpression buildIntegerLiteralExpression(int pValue) {
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, SeqSimpleType.INT, BigInteger.valueOf(pValue));
  }

  // CIdExpression =================================================================================

  /**
   * Returns a {@link CIdExpression} with a declaration of the form {@code int {pVarName} =
   * {pInitializer};}.
   */
  public static CIdExpression buildIdExpressionWithIntegerInitializer(
      String pVarName, CInitializer pInitializer) {

    CVariableDeclaration varDec =
        SeqDeclarationBuilder.buildVariableDeclaration(
            true, SeqSimpleType.INT, pVarName, pInitializer);
    return new CIdExpression(FileLocation.DUMMY, varDec);
  }

  public static CIdExpression buildIdExpression(CSimpleDeclaration pDeclaration) {
    return new CIdExpression(FileLocation.DUMMY, pDeclaration);
  }

  static ImmutableList<CIdExpression> buildScalarPcExpressions(int pNumThreads) {
    Builder<CIdExpression> rScalarPc = ImmutableList.builder();
    for (int i = 0; i < pNumThreads; i++) {
      CInitializer initializer = i == 0 ? SeqInitializer.INT_0 : SeqInitializer.INT_MINUS_1;
      rScalarPc.add(
          new CIdExpression(
              FileLocation.DUMMY,
              SeqDeclarationBuilder.buildVariableDeclaration(
                  false, SeqSimpleType.INT, SeqToken.pc + i, initializer)));
    }
    return rScalarPc.build();
  }

  // CStringLiteralExpression ======================================================================

  public static CStringLiteralExpression buildStringLiteralExpression(String pValue) {
    return new CStringLiteralExpression(FileLocation.DUMMY, pValue);
  }
}
