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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CLogicalAndExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CLogicalOrExpression;

class BitVectorAccessEvaluationBuilder {

  static Optional<CExportExpression> buildVariableOnlyEvaluationByEncoding(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build evaluation for encoding " + pOptions.bitVectorEncoding());
      case BINARY, DECIMAL, HEXADECIMAL ->
          Optional.of(
              buildFullDenseVariableOnlyEvaluation(
                  pActiveThread, pOtherThreads, pBitVectorVariables, pUtils));
      case SPARSE ->
          buildFullSparseVariableOnlyEvaluation(pActiveThread, pOtherThreads, pBitVectorVariables);
    };
  }

  static Optional<CExportExpression> buildDenseEvaluation(
      MPOROptions pOptions,
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    if (pOptions.pruneBitVectorEvaluations()) {
      return buildPrunedDenseEvaluation(
          pOtherBitVectors, pDirectMemoryLocations, pMemoryModel, pUtils);
    } else {
      return Optional.of(
          buildFullDenseEvaluation(pOtherBitVectors, pDirectMemoryLocations, pMemoryModel, pUtils));
    }
  }

  static Optional<CExportExpression> buildSparseEvaluation(
      MPOROptions pOptions,
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseBitVectorMap,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      BitVectorVariables pBitVectorVariables) {

    if (pOptions.pruneBitVectorEvaluations()) {
      return buildPrunedSparseEvaluation(
          pSparseBitVectorMap, pDirectAccessMemoryLocations, pBitVectorVariables);
    } else {
      return buildFullSparseEvaluation(
          pSparseBitVectorMap, pDirectAccessMemoryLocations, pBitVectorVariables);
    }
  }

  // Dense Access Bit Vectors ======================================================================

  private static Optional<CExportExpression> buildPrunedDenseEvaluation(
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectAccessMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    // no direct global variable accesses -> prune (either full or entirely pruned evaluation)
    if (pDirectAccessMemoryLocations.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        buildFullDenseEvaluation(
            pOtherBitVectors, pDirectAccessMemoryLocations, pMemoryModel, pUtils));
  }

  private static CExpressionWrapper buildFullDenseEvaluation(
      ImmutableSet<CExpression> pOtherBitVectors,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression directBitVector =
        BitVectorUtil.buildDirectBitVectorExpression(pMemoryModel, pDirectMemoryLocations);
    return buildFullDenseBinaryAnd(directBitVector, pOtherBitVectors, pUtils);
  }

  private static CExpressionWrapper buildFullDenseVariableOnlyEvaluation(
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

  private static CExpressionWrapper buildFullDenseBinaryAnd(
      CExpression pDirectBitVector,
      ImmutableSet<CExpression> pOtherBitVectors,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CBinaryExpressionBuilder binaryExpressionBuilder = pUtils.binaryExpressionBuilder();
    CExpression rightHandSide =
        BitVectorEvaluationUtil.binaryDisjunction(pOtherBitVectors, binaryExpressionBuilder);
    CBinaryExpression binaryExpression =
        binaryExpressionBuilder.buildBinaryExpression(
            pDirectBitVector, rightHandSide, BinaryOperator.BITWISE_AND);
    return new CExpressionWrapper(binaryExpression);
  }

  // Sparse Access Bit Vectors =====================================================================

  private static Optional<CExportExpression> buildPrunedSparseEvaluation(
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseBitVectorMap,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.areSparseAccessBitVectorsEmpty()) {
      // no sparse variables (i.e. no global variables) -> no evaluation
      return Optional.empty();
    }
    ImmutableList.Builder<CExportExpression> sparseExpressions = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
      SeqMemoryLocation memoryLocation = entry.getKey();
      // if the LHS is 0, then the entire && expression is 0 -> prune
      if (pDirectMemoryLocations.contains(memoryLocation)) {
        ImmutableList<CExpression> sparseBitVectors = pSparseBitVectorMap.get(memoryLocation);
        // if the LHS is 1, check if any expression exists for the RHS
        if (!sparseBitVectors.isEmpty()) {
          // simplify A && (B || C || ...) to just (B || C || ...)
          sparseExpressions.add(CLogicalOrExpression.of(sparseBitVectors));
        }
      }
    }
    return BitVectorEvaluationUtil.tryBuildSparseLogicalDisjunction(sparseExpressions.build());
  }

  private static Optional<CExportExpression> buildFullSparseEvaluation(
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseBitVectorMap,
      ImmutableSet<SeqMemoryLocation> pDirectMemoryLocations,
      BitVectorVariables pBitVectorVariables) {

    if (pBitVectorVariables.areSparseAccessBitVectorsEmpty()) {
      // no sparse variables (i.e. no global variables) -> no evaluation
      return Optional.empty();
    }
    ImmutableList.Builder<CExportExpression> sparseExpressions = ImmutableList.builder();
    for (SeqMemoryLocation memoryLocation :
        pBitVectorVariables.getSparseAccessBitVectors().keySet()) {
      CIntegerLiteralExpression directBitVector =
          BitVectorEvaluationUtil.buildSparseDirectBitVector(
              memoryLocation, pDirectMemoryLocations);
      CExportExpression logicalAnd =
          buildSingleSparseLogicalAndExpression(
              pSparseBitVectorMap, directBitVector, memoryLocation);
      sparseExpressions.add(logicalAnd);
    }
    // create disjunction of logical not: (A && (B || C)) || (A' && (B' || C'))
    return Optional.of(new CLogicalOrExpression(sparseExpressions.build()));
  }

  /**
   * Note that the 'full' evaluation can still be pruned entirely if {@link
   * MPOROptions#pruneSparseBitVectors()} is enabled.
   */
  private static Optional<CExportExpression> buildFullSparseVariableOnlyEvaluation(
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables) {

    ImmutableListMultimap<SeqMemoryLocation, CExpression> sparseBitVectorMap =
        BitVectorEvaluationUtil.mapMemoryLocationsToSparseBitVectorsByAccessType(
            pOtherThreads, pBitVectorVariables, MemoryAccessType.ACCESS);
    List<CExportExpression> sparseExpressions = new ArrayList<>();
    for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
      CIdExpression directBitVector =
          entry.getValue().getVariablesByReachType(ReachType.DIRECT).get(pActiveThread);
      CExportExpression sparseExpression =
          buildSingleSparseLogicalAndExpression(
              sparseBitVectorMap, Objects.requireNonNull(directBitVector), entry.getKey());
      sparseExpressions.add(sparseExpression);
    }
    // create disjunction of logical not: (A && (B || C)) || (A' && (B' || C'))
    return sparseExpressions.isEmpty()
        ? Optional.empty()
        : Optional.of(new CLogicalOrExpression(ImmutableList.copyOf(sparseExpressions)));
  }

  private static CExportExpression buildSingleSparseLogicalAndExpression(
      ImmutableListMultimap<SeqMemoryLocation, CExpression> pSparseBitVectorMap,
      CExpression pDirectBitVector,
      SeqMemoryLocation pMemoryLocation) {

    // create logical disjunction -> (B || C || ...)
    Optional<CExportExpression> disjunction =
        BitVectorEvaluationUtil.tryBuildSparseLogicalDisjunction(
            pSparseBitVectorMap.get(pMemoryLocation).stream()
                .map(CExpressionWrapper::new)
                .collect(ImmutableList.toImmutableList()));
    CExportExpression directBitVector = new CExpressionWrapper(pDirectBitVector);

    // if the logical disjunction is empty, return (A), otherwise return (A && (B || ...))
    return disjunction.isEmpty()
        ? directBitVector
        : CLogicalAndExpression.of(directBitVector, disjunction.orElseThrow());
  }
}
