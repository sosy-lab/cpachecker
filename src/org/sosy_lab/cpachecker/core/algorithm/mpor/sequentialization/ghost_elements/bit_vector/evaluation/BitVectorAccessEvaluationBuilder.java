// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeUtil;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;

class BitVectorAccessEvaluationBuilder {

  static BitVectorEvaluationExpression buildVariableOnlyEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildFullDenseVariableOnlyEvaluation(
              pActiveThread, pOtherThreads, pBitVectorVariables, pUtils);
      case SPARSE ->
          buildFullSparseVariableOnlyEvaluation(
              pActiveThread, pOtherThreads, pBitVectorVariables, pUtils);
    };
  }

  static BitVectorEvaluationExpression buildDenseEvaluation(
      MPOROptions pOptions,
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    if (pOptions.pruneBitVectorEvaluations) {
      return buildPrunedDenseEvaluation(
          pOtherBitVectors, pDirectMemoryLocations, pMemoryModel, pUtils);
    } else {
      return buildFullDenseEvaluation(
          pOtherBitVectors, pDirectMemoryLocations, pMemoryModel, pUtils);
    }
  }

  static BitVectorEvaluationExpression buildSparseEvaluation(
      MPOROptions pOptions,
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseBitVectorMap,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils) {

    if (pOptions.pruneBitVectorEvaluations) {
      return buildPrunedSparseEvaluation(
          pSparseBitVectorMap, pDirectAccessMemoryLocations, pBitVectorVariables, pUtils);
    } else {
      return buildFullSparseEvaluation(
          pSparseBitVectorMap, pDirectAccessMemoryLocations, pBitVectorVariables, pUtils);
    }
  }

  // Dense Access Bit Vectors ======================================================================

  private static BitVectorEvaluationExpression buildPrunedDenseEvaluation(
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    // no direct global variable accesses -> prune (either full or entirely pruned evaluation)
    if (pDirectAccessMemoryLocations.isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    }
    return buildFullDenseEvaluation(
        pOtherBitVectors, pDirectAccessMemoryLocations, pMemoryModel, pUtils);
  }

  private static BitVectorEvaluationExpression buildFullDenseEvaluation(
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression directBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(pMemoryModel, pDirectMemoryLocations);
    return buildFullDenseBinaryAnd(directBitVector, pOtherBitVectors, pUtils);
  }

  private static BitVectorEvaluationExpression buildFullDenseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CExpression directBitVector =
        pBitVectorVariables.getDenseBitVector(
            pActiveThread, MemoryAccessType.ACCESS, ReachType.DIRECT);
    ImmutableSet<CExpression> otherReachableBitVectors =
        pBitVectorVariables.getOtherDenseReachableBitVectorsByAccessType(
            MemoryAccessType.ACCESS, pOtherThreads);
    return buildFullDenseBinaryAnd(directBitVector, otherReachableBitVectors, pUtils);
  }

  private static BitVectorEvaluationExpression buildFullDenseBinaryAnd(
      CExpression pDirectBitVector,
      ImmutableSet<CExpression> pOtherBitVectors,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CBinaryExpressionBuilder binaryExpressionBuilder = pUtils.getBinaryExpressionBuilder();
    CExpression rightHandSide =
        BitVectorEvaluationUtil.binaryDisjunction(pOtherBitVectors, binaryExpressionBuilder);
    CBinaryExpression binaryExpression =
        binaryExpressionBuilder.buildBinaryExpression(
            pDirectBitVector, rightHandSide, BinaryOperator.BINARY_AND);
    return new BitVectorEvaluationExpression(binaryExpression, pUtils);
  }

  // Sparse Access Bit Vectors =====================================================================

  private static BitVectorEvaluationExpression buildPrunedSparseEvaluation(
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseBitVectorMap,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils) {

    if (pBitVectorVariables.areSparseAccessBitVectorsEmpty()) {
      // no sparse variables (i.e. no global variables) -> no evaluation
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<ExpressionTree<AExpression>> sparseExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
      SeqMemoryLocation memoryLocation = entry.getKey();
      // if the LHS is 0, then the entire && expression is 0 -> prune
      if (pDirectMemoryLocations.contains(memoryLocation)) {
        // if the LHS is 1, check if any expression exists for the RHS
        if (!pSparseBitVectorMap.get(memoryLocation).isEmpty()) {
          // simplify A && (B || C || ...) to just (B || C || ...)
          ExpressionTree<AExpression> logicalDisjunction =
              BitVectorEvaluationUtil.logicalDisjunction(
                  ExpressionTreeUtil.toExpressionTree(pSparseBitVectorMap.get(memoryLocation)));
          sparseExpressions.add(logicalDisjunction);
        }
      }
    }
    return BitVectorEvaluationUtil.buildSparseLogicalDisjunction(sparseExpressions.build(), pUtils);
  }

  private static BitVectorEvaluationExpression buildFullSparseEvaluation(
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseBitVectorMap,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils) {

    if (pBitVectorVariables.areSparseAccessBitVectorsEmpty()) {
      // no sparse variables (i.e. no global variables) -> no evaluation
      return BitVectorEvaluationExpression.empty();
    }
    ImmutableList.Builder<ExpressionTree<AExpression>> sparseExpressions = ImmutableList.builder();
    for (SeqMemoryLocation memoryLocation :
        pBitVectorVariables.getSparseAccessBitVectors().keySet()) {
      CIntegerLiteralExpression directBitVector =
          BitVectorEvaluationUtil.buildSparseDirectBitVector(
              memoryLocation, pDirectMemoryLocations);
      ExpressionTree<AExpression> logicalAnd =
          buildSingleSparseLogicalAndExpression(
              pSparseBitVectorMap, directBitVector, memoryLocation);
      sparseExpressions.add(logicalAnd);
    }
    // create disjunction of logical not: (A && (B || C)) || (A' && (B' || C'))
    ExpressionTree<AExpression> logicalDisjunction =
        BitVectorEvaluationUtil.logicalDisjunction(sparseExpressions.build());
    return new BitVectorEvaluationExpression(logicalDisjunction, pUtils);
  }

  private static BitVectorEvaluationExpression buildFullSparseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils) {

    ImmutableListMultimap<SeqMemoryLocation, AExpression> sparseBitVectorMap =
        BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
            pOtherThreads, pBitVectorVariables, MemoryAccessType.ACCESS);
    ImmutableList.Builder<ExpressionTree<AExpression>> sparseExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
      CIdExpression directBitVector =
          entry.getValue().getVariablesByReachType(ReachType.DIRECT).get(pActiveThread);
      ExpressionTree<AExpression> logicalAnd =
          buildSingleSparseLogicalAndExpression(
              sparseBitVectorMap, directBitVector, entry.getKey());
      sparseExpressions.add(logicalAnd);
    }
    // create disjunction of logical not: (A && (B || C)) || (A' && (B' || C'))
    Optional<ExpressionTree<AExpression>> logicalDisjunction =
        BitVectorEvaluationUtil.tryLogicalDisjunction(sparseExpressions.build());
    if (logicalDisjunction.isEmpty()) {
      return BitVectorEvaluationExpression.empty();
    } else {
      return new BitVectorEvaluationExpression(logicalDisjunction.orElseThrow(), pUtils);
    }
  }

  private static ExpressionTree<AExpression> buildSingleSparseLogicalAndExpression(
      ImmutableListMultimap<SeqMemoryLocation, AExpression> pSparseBitVectorMap,
      CExpression pDirectBitVector,
      SeqMemoryLocation pMemoryLocation) {

    // create logical disjunction -> (B || C || ...)
    ImmutableList<AExpression> sparseBitVectors = pSparseBitVectorMap.get(pMemoryLocation);
    if (sparseBitVectors.isEmpty()) {
      // if the logical disjunction is empty, return just (A) instead of (A && (B || C || ...))
      return LeafExpression.of(pDirectBitVector);
    }
    ExpressionTree<AExpression> disjunction =
        BitVectorEvaluationUtil.logicalDisjunction(
            ExpressionTreeUtil.toExpressionTree(sparseBitVectors));
    // create logical and -> (A && (B || C || ...))
    return And.of(LeafExpression.of(pDirectBitVector), disjunction);
  }
}
