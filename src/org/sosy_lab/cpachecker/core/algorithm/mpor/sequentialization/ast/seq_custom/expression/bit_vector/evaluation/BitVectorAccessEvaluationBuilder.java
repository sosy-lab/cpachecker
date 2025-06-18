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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.ScalarBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorAccessEvaluationBuilder {

  /**
   * Builds a pruned evaluation expression for the given bit vectors based on the direct access
   * variables.
   */
  static BitVectorEvaluationExpression buildEvaluationByEncoding(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectVariables,
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
              pOtherThreads, pDirectVariables, pBitVectorVariables, pBinaryExpressionBuilder);
        } else {
          yield buildFullDenseEvaluation(
              pOtherThreads, pDirectVariables, pBitVectorVariables, pBinaryExpressionBuilder);
        }
      }
      case SCALAR -> {
        if (pOptions.bitVectorEvaluationPrune) {
          yield buildPrunedScalarEvaluation(pOtherThreads, pDirectVariables, pBitVectorVariables);
        } else {
          yield buildFullScalarEvaluation(pOtherThreads, pDirectVariables, pBitVectorVariables);
        }
      }
    };
  }

  // Dense Access Bit Vectors ======================================================================

  private static BitVectorEvaluationExpression buildPrunedDenseEvaluation(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    // no direct global variable accesses -> prune (either full or entirely pruned evaluation)
    if (pDirectVariables.isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    return buildFullDenseEvaluation(
        pOtherThreads, pDirectVariables, pBitVectorVariables, pBinaryExpressionBuilder);
  }

  private static BitVectorEvaluationExpression buildFullDenseEvaluation(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression directBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(
            pBitVectorVariables.globalVariableIds, pDirectVariables);
    ImmutableSet<CExpression> otherBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            BitVectorAccessType.ACCESS, pOtherThreads);
    CExpression rightHandSide =
        BitVectorEvaluationUtil.binaryDisjunction(otherBitVectors, pBinaryExpressionBuilder);
    CBinaryExpression binaryExpression =
        pBinaryExpressionBuilder.buildBinaryExpression(
            directBitVector, rightHandSide, BinaryOperator.BINARY_AND);
    SeqLogicalNotExpression logicalNot = new SeqLogicalNotExpression(binaryExpression);
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.of(logicalNot));
  }

  // Scalar Access Bit Vectors =====================================================================

  private static BitVectorEvaluationExpression buildPrunedScalarEvaluation(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectVariables,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.scalarAccessBitVectors.isEmpty()) {
      // no scalar variables (i.e. no global variables) -> no evaluation
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<SeqExpression> scalarExpressions = ImmutableList.builder();
    ImmutableMap<CVariableDeclaration, ScalarBitVector> scalarBitVectors =
        pBitVectorVariables.getScalarBitVectorsByAccessType(BitVectorAccessType.ACCESS);
    for (var entry : scalarBitVectors.entrySet()) {
      CVariableDeclaration globalVariable = entry.getKey();
      ScalarBitVector scalarBitVector = entry.getValue();
      ImmutableMap<MPORThread, CIdExpression> accessVariables = scalarBitVector.variables;
      // if the LHS (current variable) is not accessed, then the entire && expression is 0 -> prune
      if (pDirectVariables.contains(globalVariable)) {
        // if the LHS is 1, then we can simplify A && (B || C || ...) to just (B || C || ...)
        ImmutableList<SeqExpression> otherVariables =
            BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
                pOtherThreads, accessVariables);
        // create logical not -> !(B || C || ...)
        SeqLogicalNotExpression logicalNot =
            new SeqLogicalNotExpression(BitVectorEvaluationUtil.logicalDisjunction(otherVariables));
        scalarExpressions.add(logicalNot);
      }
    }
    ImmutableList<SeqExpression> expressions = scalarExpressions.build();
    return expressions.isEmpty()
        ? BitVectorEvaluationExpression.empty()
        : BitVectorEvaluationUtil.buildScalarLogicalConjunction(expressions);
  }

  private static BitVectorEvaluationExpression buildFullScalarEvaluation(
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectVariables,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.scalarAccessBitVectors.isEmpty()) {
      // no scalar variables (i.e. no global variables) -> no evaluation
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<SeqExpression> scalarExpressions = ImmutableList.builder();
    ImmutableMap<CVariableDeclaration, ScalarBitVector> scalarBitVectors =
        pBitVectorVariables.getScalarBitVectorsByAccessType(BitVectorAccessType.ACCESS);
    for (var entry : scalarBitVectors.entrySet()) {
      CVariableDeclaration globalVariable = entry.getKey();
      ImmutableMap<MPORThread, CIdExpression> accessVariables = entry.getValue().variables;
      ImmutableList<SeqExpression> otherVariables =
          BitVectorEvaluationUtil.convertOtherVariablesToSeqExpression(
              pOtherThreads, accessVariables);
      // create logical disjunction -> (B || C || ...)
      SeqExpression disjunction = BitVectorEvaluationUtil.logicalDisjunction(otherVariables);
      // create logical and -> (A && (B || C || ...))
      SeqExpression directBitVector =
          BitVectorEvaluationUtil.buildScalarDirectBitVector(globalVariable, pDirectVariables);
      SeqLogicalAndExpression logicalAnd =
          new SeqLogicalAndExpression(directBitVector, disjunction);
      // create logical not -> !(A && (B || C || ...))
      scalarExpressions.add(new SeqLogicalNotExpression(logicalAnd));
    }
    return BitVectorEvaluationUtil.buildScalarLogicalConjunction(scalarExpressions.build());
  }
}
