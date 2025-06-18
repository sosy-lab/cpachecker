// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.GlobalVariableFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorEvaluationBuilder {

  public static BitVectorEvaluationExpression buildEvaluationByDirectVariableAccesses(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pTargetBlock,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(
        pOptions.conflictReduction || pOptions.bitVectorReduction,
        "either conflictReduction or bitVectorReduction must be enabled");

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "conflictReduction must be enabled to build evaluation expression");
      case ACCESS_ONLY -> {
        ImmutableSet<CVariableDeclaration> directAccessVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap, pTargetBlock, BitVectorAccessType.ACCESS);
        yield BitVectorEvaluationBuilder.buildEvaluationByReduction(
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
                pLabelBlockMap, pTargetBlock, BitVectorAccessType.READ);
        ImmutableSet<CVariableDeclaration> directWriteVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap, pTargetBlock, BitVectorAccessType.WRITE);
        yield BitVectorEvaluationBuilder.buildEvaluationByReduction(
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
          BitVectorAccessEvaluationBuilder.buildEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectAccessVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
      case READ_AND_WRITE ->
          BitVectorReadWriteEvaluationBuilder.buildEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectReadVariables,
              pDirectWriteVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
    };
  }
}
