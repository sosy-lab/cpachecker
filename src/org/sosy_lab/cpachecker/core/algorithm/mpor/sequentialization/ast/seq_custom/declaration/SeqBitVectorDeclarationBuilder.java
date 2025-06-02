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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.ScalarBitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.DenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.ScalarBitVector;
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
      ImmutableSet<MPORThread> pThreads) {

    if (!pOptions.bitVectorReduction.isEnabled() && !pOptions.bitVectorEncoding.isEnabled()) {
      return ImmutableList.of();
    }
    return switch (pOptions.bitVectorEncoding) {
      case NONE -> ImmutableList.of();
      case BINARY, HEXADECIMAL ->
          buildDenseBitVectorDeclarationsByReduction(pOptions, pBitVectorVariables.orElseThrow());
      case SCALAR ->
          buildScalarBitVectorDeclarationsByReduction(
              pOptions, pBitVectorVariables.orElseThrow(), pThreads);
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration> buildDenseBitVectorDeclarationsByReduction(
      MPOROptions pOptions, BitVectorVariables pBitVectorVariables) {

    return switch (pOptions.bitVectorReduction) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          buildDenseBitVectorDeclarationsByAccessType(
              pOptions, pBitVectorVariables, BitVectorAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
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

    int binaryLength = BitVectorUtil.getBinaryLength(pBitVectorVariables.numGlobalVariables);
    BitVectorDataType type = BitVectorUtil.getDataTypeByLength(binaryLength);
    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (DenseBitVector denseBitVector :
        pBitVectorVariables.getDenseBitVectorsByAccessType(pAccessType)) {

      BitVectorExpression initializer =
          BitVectorUtil.buildBitVectorExpression(
              pOptions, pBitVectorVariables.globalVariableIds, ImmutableSet.of());
      SeqBitVectorDeclaration bitVectorDeclaration =
          new SeqBitVectorDeclaration(type, denseBitVector.reachableVariable, initializer);

      rDeclarations.add(bitVectorDeclaration);
    }
    return rDeclarations.build();
  }

  private static ImmutableList<SeqBitVectorDeclaration> buildScalarBitVectorDeclarationsByReduction(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      ImmutableSet<MPORThread> pThreads) {

    return switch (pOptions.bitVectorReduction) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          createScalarBitVectorDeclarations(
              pBitVectorVariables.scalarAccessBitVectors.orElseThrow().values(), pThreads);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(
                  createScalarBitVectorDeclarations(
                      pBitVectorVariables.scalarReadBitVectors.orElseThrow().values(), pThreads))
              .addAll(
                  createScalarBitVectorDeclarations(
                      pBitVectorVariables.scalarWriteBitVectors.orElseThrow().values(), pThreads))
              .build();
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration> createScalarBitVectorDeclarations(
      ImmutableCollection<ScalarBitVector> pScalarBitVectors, ImmutableSet<MPORThread> pThreads) {

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (ScalarBitVector scalarBitVector : pScalarBitVectors) {
      ImmutableMap<MPORThread, CIdExpression> accessVariables = scalarBitVector.variables;
      for (MPORThread thread : pThreads) {
        assert accessVariables.containsKey(thread) : "thread must have access variable";
        CIdExpression variable = accessVariables.get(thread);
        ScalarBitVectorExpression initializer = new ScalarBitVectorExpression(false);
        SeqBitVectorDeclaration declaration =
            new SeqBitVectorDeclaration(BitVectorDataType.__UINT8_T, variable, initializer);
        rDeclarations.add(declaration);
      }
    }
    return rDeclarations.build();
  }
}
