// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.declaration;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.value.BitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.value.SparseBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.DenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqBitVectorDeclarationBuilder {

  /**
   * Returns, if enabled, the list of bit vector declarations based on {@code pOptions}. Note that
   * bit vectors are always initialized with {@code 0} for all indices, the actual assignment based
   * on the threads first statement is done when a thread is marked as active / created.
   */
  public static ImmutableList<SeqBitVectorDeclaration> buildBitVectorDeclarationsByEncoding(
      MPOROptions pOptions,
      Optional<BitVectorVariables> pBitVectorVariables,
      ImmutableList<MPORThread> pThreads) {

    if (!pOptions.areBitVectorsEnabled()) {
      return ImmutableList.of();
    }
    return switch (pOptions.bitVectorEncoding) {
      case NONE -> ImmutableList.of();
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildDenseBitVectorDeclarationsByReduction(pOptions, pBitVectorVariables.orElseThrow());
      case SPARSE ->
          buildSparseBitVectorDeclarationsByReduction(
              pOptions, pBitVectorVariables.orElseThrow(), pThreads);
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration> buildDenseBitVectorDeclarationsByReduction(
      MPOROptions pOptions, BitVectorVariables pBitVectorVariables) {

    return switch (pOptions.reductionMode) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          buildDenseBitVectorDeclarationsByAccessType(
              pOptions, pBitVectorVariables, BitVectorAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(
                  buildDenseBitVectorDeclarationsByAccessType(
                      pOptions, pBitVectorVariables, BitVectorAccessType.ACCESS))
              .addAll(
                  buildDenseBitVectorDeclarationsByAccessType(
                      pOptions, pBitVectorVariables, BitVectorAccessType.READ))
              .addAll(
                  buildDenseBitVectorDeclarationsByAccessType(
                      pOptions, pBitVectorVariables, BitVectorAccessType.WRITE))
              .build();
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration> buildDenseBitVectorDeclarationsByAccessType(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      BitVectorAccessType pAccessType) {

    int binaryLength = BitVectorUtil.getBinaryLength(pBitVectorVariables.getMemoryLocationAmount());
    BitVectorDataType type = BitVectorUtil.getDataTypeByLength(binaryLength);
    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (DenseBitVector denseBitVector :
        pBitVectorVariables.getDenseBitVectorsByAccessType(pAccessType)) {

      BitVectorValueExpression initializer =
          BitVectorUtil.buildBitVectorExpression(
              pOptions, pBitVectorVariables.getMemoryLocationIds(), ImmutableSet.of());
      if (pOptions.kIgnoreZeroReduction && denseBitVector.directVariable.isPresent()) {
        // direct bit vector
        SeqBitVectorDeclaration directDeclaration =
            new SeqBitVectorDeclaration(
                type, denseBitVector.directVariable.orElseThrow(), initializer);
        rDeclarations.add(directDeclaration);
      }
      if (denseBitVector.reachableVariable.isPresent()) {
        // reachable bit vector
        SeqBitVectorDeclaration reachableDeclaration =
            new SeqBitVectorDeclaration(
                type, denseBitVector.reachableVariable.orElseThrow(), initializer);
        rDeclarations.add(reachableDeclaration);
      }
    }
    if (pBitVectorVariables.isLastDenseBitVectorPresentByAccessType(pAccessType)) {
      LastDenseBitVector lastDenseBitVector =
          pBitVectorVariables.getLastDenseBitVectorByAccessType(pAccessType);
      BitVectorValueExpression initializer =
          BitVectorUtil.buildBitVectorExpression(
              pOptions, pBitVectorVariables.getMemoryLocationIds(), ImmutableSet.of());
      // reachable last bit vector
      SeqBitVectorDeclaration reachableDeclaration =
          new SeqBitVectorDeclaration(type, lastDenseBitVector.reachableVariable, initializer);
      rDeclarations.add(reachableDeclaration);
    }
    return rDeclarations.build();
  }

  private static ImmutableList<SeqBitVectorDeclaration> buildSparseBitVectorDeclarationsByReduction(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<MPORThread> pThreads) {

    return switch (pOptions.reductionMode) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          createSparseBitVectorDeclarations(
              pBitVectorVariables.getSparseAccessBitVectors().values(),
              pBitVectorVariables.tryGetLastSparseBitVectorByAccessType(BitVectorAccessType.ACCESS),
              pThreads);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(
                  createSparseBitVectorDeclarations(
                      pBitVectorVariables.getSparseAccessBitVectors().values(),
                      pBitVectorVariables.tryGetLastSparseBitVectorByAccessType(
                          BitVectorAccessType.ACCESS),
                      pThreads))
              .addAll(
                  createSparseBitVectorDeclarations(
                      pBitVectorVariables.getSparseWriteBitVectors().values(),
                      pBitVectorVariables.tryGetLastSparseBitVectorByAccessType(
                          BitVectorAccessType.WRITE),
                      pThreads))
              .build();
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration> createSparseBitVectorDeclarations(
      ImmutableCollection<SparseBitVector> pSparseBitVectors,
      Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>> pLastSparseBitVectors,
      ImmutableList<MPORThread> pThreads) {

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (SparseBitVector sparseBitVector : pSparseBitVectors) {
      ImmutableMap<MPORThread, CIdExpression> accessVariables = sparseBitVector.variables;
      for (MPORThread thread : pThreads) {
        assert accessVariables.containsKey(thread) : "thread must have access variable";
        CIdExpression variable = accessVariables.get(thread);
        SparseBitVectorValueExpression initializer = new SparseBitVectorValueExpression(false);
        SeqBitVectorDeclaration declaration =
            new SeqBitVectorDeclaration(BitVectorDataType.__UINT8_T, variable, initializer);
        rDeclarations.add(declaration);
      }
    }
    if (pLastSparseBitVectors.isPresent()) {
      for (LastSparseBitVector sparseBitVector : pLastSparseBitVectors.orElseThrow().values()) {
        SparseBitVectorValueExpression initializer = new SparseBitVectorValueExpression(false);
        SeqBitVectorDeclaration declaration =
            new SeqBitVectorDeclaration(
                BitVectorDataType.__UINT8_T, sparseBitVector.variable, initializer);
        rDeclarations.add(declaration);
      }
    }
    return rDeclarations.build();
  }
}
