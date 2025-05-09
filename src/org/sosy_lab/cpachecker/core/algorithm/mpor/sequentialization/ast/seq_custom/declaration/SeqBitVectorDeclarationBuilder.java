// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.declaration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.ScalarBitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
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
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses) {

    if (!pOptions.bitVectorReduction.isEnabled() && !pOptions.bitVectorEncoding.isEnabled()) {
      return ImmutableList.of();
    }
    return switch (pOptions.bitVectorEncoding) {
      case NONE -> ImmutableList.of();
      case BINARY, HEXADECIMAL ->
          createBitVectorDeclarationsByReduction(pOptions, pBitVectorVariables.orElseThrow());
      case SCALAR ->
          createScalarBitVectorDeclarationsByReduction(
              pOptions, pBitVectorVariables.orElseThrow(), pClauses);
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration> createBitVectorDeclarationsByReduction(
      MPOROptions pOptions, BitVectorVariables pBitVectorVariables) {

    return switch (pOptions.bitVectorReduction) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          createDenseBitVectorDeclarationsByAccessType(
              pOptions, pBitVectorVariables, BitVectorAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(
                  createDenseBitVectorDeclarationsByAccessType(
                      pOptions, pBitVectorVariables, BitVectorAccessType.READ))
              .addAll(
                  createDenseBitVectorDeclarationsByAccessType(
                      pOptions, pBitVectorVariables, BitVectorAccessType.WRITE))
              .build();
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration>
      createDenseBitVectorDeclarationsByAccessType(
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
              pOptions, pBitVectorVariables.globalVariableIds, ImmutableList.of());
      SeqBitVectorDeclaration declaration =
          new SeqBitVectorDeclaration(type, denseBitVector.idExpression, initializer);
      rDeclarations.add(declaration);
    }
    return rDeclarations.build();
  }

  private static ImmutableList<SeqBitVectorDeclaration>
      createScalarBitVectorDeclarationsByReduction(
          MPOROptions pOptions,
          BitVectorVariables pBitVectorVariables,
          ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses) {

    return switch (pOptions.bitVectorReduction) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          createScalarBitVectorDeclarations(
              pBitVectorVariables.scalarAccessBitVectors.orElseThrow(), pClauses);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(
                  createScalarBitVectorDeclarations(
                      pBitVectorVariables.scalarReadBitVectors.orElseThrow(), pClauses))
              .addAll(
                  createScalarBitVectorDeclarations(
                      pBitVectorVariables.scalarWriteBitVectors.orElseThrow(), pClauses))
              .build();
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration> createScalarBitVectorDeclarations(
      ImmutableMap<CVariableDeclaration, ScalarBitVector> pScalarBitVectorAccessVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (var entryB : pScalarBitVectorAccessVariables.entrySet()) {
      ImmutableMap<MPORThread, CIdExpression> accessVariables = entryB.getValue().variables;
      for (var entryA : pCaseClauses.entrySet()) {
        MPORThread thread = entryA.getKey();
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
