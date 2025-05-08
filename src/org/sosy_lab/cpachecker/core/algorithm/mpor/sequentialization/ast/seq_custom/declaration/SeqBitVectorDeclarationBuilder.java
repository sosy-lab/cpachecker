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
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.ScalarBitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.DenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.ScalarBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqBitVectorDeclarationBuilder {

  /**
   * Returns, if enabled, the list of bit vector declarations based on {@code pOptions}. This method
   * uses {@code pCaseClauses} to initialize the bit vectors based on the first statement of the
   * thread, i.e. which global variables it accesses/reads/writes.
   */
  public static ImmutableList<SeqBitVectorDeclaration> buildBitVectorDeclarationsByEncoding(
      MPOROptions pOptions,
      Optional<BitVectorVariables> pBitVectorVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    if (!pOptions.bitVectorReduction.isEnabled() && !pOptions.bitVectorEncoding.isEnabled()) {
      return ImmutableList.of();
    }
    return switch (pOptions.bitVectorEncoding) {
      case NONE -> ImmutableList.of();
      case BINARY, HEXADECIMAL ->
          createBitVectorDeclarationsByReduction(
              pOptions, pBitVectorVariables.orElseThrow(), pCaseClauses);
      case SCALAR ->
          createScalarBitVectorDeclarationsByReduction(
              pOptions, pBitVectorVariables.orElseThrow(), pCaseClauses);
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration> createBitVectorDeclarationsByReduction(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    return switch (pOptions.bitVectorReduction) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          createDenseBitVectorDeclarationsByAccessType(
              pOptions, pBitVectorVariables, BitVectorAccessType.ACCESS, pCaseClauses);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(
                  createDenseBitVectorDeclarationsByAccessType(
                      pOptions, pBitVectorVariables, BitVectorAccessType.READ, pCaseClauses))
              .addAll(
                  createDenseBitVectorDeclarationsByAccessType(
                      pOptions, pBitVectorVariables, BitVectorAccessType.WRITE, pCaseClauses))
              .build();
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration>
      createDenseBitVectorDeclarationsByAccessType(
          MPOROptions pOptions,
          BitVectorVariables pBitVectorVariables,
          BitVectorAccessType pAccessType,
          ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    int binaryLength = BitVectorUtil.getBinaryLength(pBitVectorVariables.numGlobalVariables);
    BitVectorDataType type = BitVectorUtil.getDataTypeByLength(binaryLength);
    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (DenseBitVector denseBitVector :
        pBitVectorVariables.getDenseBitVectorsByAccessType(pAccessType)) {

      MPORThread thread = denseBitVector.thread;
      SeqThreadStatementClause firstCase = Objects.requireNonNull(pCaseClauses.get(thread)).get(0);
      ImmutableList<CVariableDeclaration> firstCaseGlobalVariables =
          SeqThreadStatementClauseUtil.findGlobalVariablesInCaseClauseByAccessType(
              firstCase, pAccessType);
      BitVectorExpression initializer =
          BitVectorUtil.buildBitVectorExpression(
              pOptions, pBitVectorVariables.globalVariableIds, firstCaseGlobalVariables);
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
          ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    return switch (pOptions.bitVectorReduction) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          createScalarBitVectorDeclarations(
              pBitVectorVariables.scalarAccessBitVectors.orElseThrow(), pCaseClauses);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(
                  createScalarBitVectorDeclarations(
                      pBitVectorVariables.scalarReadBitVectors.orElseThrow(), pCaseClauses))
              .addAll(
                  createScalarBitVectorDeclarations(
                      pBitVectorVariables.scalarWriteBitVectors.orElseThrow(), pCaseClauses))
              .build();
    };
  }

  private static ImmutableList<SeqBitVectorDeclaration> createScalarBitVectorDeclarations(
      ImmutableMap<CVariableDeclaration, ScalarBitVector> pScalarBitVectorAccessVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (var entryB : pScalarBitVectorAccessVariables.entrySet()) {
      ImmutableMap<MPORThread, CIdExpression> accessVariables = entryB.getValue().variables;
      BitVectorAccessType accessType = entryB.getValue().accessType;
      for (var entryA : pCaseClauses.entrySet()) {
        MPORThread thread = entryA.getKey();
        SeqThreadStatementClause firstCase = Objects.requireNonNull(entryA.getValue()).get(0);
        ImmutableList<CVariableDeclaration> firstCaseGlobalVariables =
            SeqThreadStatementClauseUtil.findGlobalVariablesInCaseClauseByAccessType(
                firstCase, accessType);
        assert accessVariables.containsKey(thread) : "thread must have access variable";
        CIdExpression variable = accessVariables.get(thread);
        boolean value = firstCaseGlobalVariables.contains(entryB.getKey());
        ScalarBitVectorExpression initializer = new ScalarBitVectorExpression(value);
        SeqBitVectorDeclaration declaration =
            new SeqBitVectorDeclaration(BitVectorDataType.__UINT8_T, variable, initializer);
        rDeclarations.add(declaration);
      }
    }
    return rDeclarations.build();
  }
}
