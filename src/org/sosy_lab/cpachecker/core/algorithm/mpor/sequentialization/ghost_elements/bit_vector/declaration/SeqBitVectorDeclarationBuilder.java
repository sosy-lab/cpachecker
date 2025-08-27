// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.declaration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.DenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.BitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.SparseBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
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
      Optional<MemoryModel> pMemoryModel,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    if (!pOptions.areBitVectorsEnabled()) {
      return ImmutableList.of();
    }
    return switch (pOptions.bitVectorEncoding) {
      case NONE -> ImmutableList.of();
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildDenseBitVectorDeclarationsByReduction(
              pOptions, pBitVectorVariables.orElseThrow(), pMemoryModel.orElseThrow(), pClauses);
      case SPARSE ->
          buildSparseBitVectorDeclarationsByReduction(
              pOptions, pBitVectorVariables.orElseThrow(), pMemoryModel.orElseThrow(), pClauses);
    };
  }

  // DENSE =========================================================================================

  private static ImmutableList<SeqBitVectorDeclaration> buildDenseBitVectorDeclarationsByReduction(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    return switch (pOptions.reductionMode) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          buildDenseBitVectorDeclarationsByAccessType(
              pOptions, pBitVectorVariables, pMemoryModel, pClauses, MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(
                  buildDenseBitVectorDeclarationsByAccessType(
                      pOptions,
                      pBitVectorVariables,
                      pMemoryModel,
                      pClauses,
                      MemoryAccessType.ACCESS))
              .addAll(
                  buildDenseBitVectorDeclarationsByAccessType(
                      pOptions, pBitVectorVariables, pMemoryModel, pClauses, MemoryAccessType.READ))
              .addAll(
                  buildDenseBitVectorDeclarationsByAccessType(
                      pOptions,
                      pBitVectorVariables,
                      pMemoryModel,
                      pClauses,
                      MemoryAccessType.WRITE))
              .build();
    };
  }

  // TODO split into separate functions
  private static ImmutableList<SeqBitVectorDeclaration> buildDenseBitVectorDeclarationsByAccessType(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      MemoryAccessType pAccessType) {

    int binaryLength = BitVectorUtil.getBinaryLength(pMemoryModel);
    BitVectorDataType type = BitVectorUtil.getDataTypeByLength(binaryLength);
    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (DenseBitVector denseBitVector :
        pBitVectorVariables.getDenseBitVectorsByAccessType(pAccessType)) {
      MPORThread thread = denseBitVector.thread;
      if (pOptions.kIgnoreZeroReduction && denseBitVector.directVariable.isPresent()) {
        ImmutableSet<MemoryLocation> directMemoryLocations =
            getDirectMemoryLocationsByAccessType(pMemoryModel, pClauses.get(thread), pAccessType);
        BitVectorValueExpression directInitializer =
            BitVectorUtil.buildBitVectorExpression(pOptions, pMemoryModel, directMemoryLocations);
        // direct bit vector
        SeqBitVectorDeclaration directDeclaration =
            new SeqBitVectorDeclaration(
                type, denseBitVector.directVariable.orElseThrow(), directInitializer);
        rDeclarations.add(directDeclaration);
      }
      if (denseBitVector.reachableVariable.isPresent()) {
        // TODO we can optimize here by saving the 0 initializers and leaving them out entirely
        //  or not write them ever again
        ImmutableSet<MemoryLocation> reachableMemoryLocations =
            getReachableMemoryLocationsByAccessType(
                pMemoryModel, pClauses.get(thread), pAccessType);
        BitVectorValueExpression reachableInitializer =
            BitVectorUtil.buildBitVectorExpression(
                pOptions, pMemoryModel, reachableMemoryLocations);
        // reachable bit vector
        SeqBitVectorDeclaration reachableDeclaration =
            new SeqBitVectorDeclaration(
                type, denseBitVector.reachableVariable.orElseThrow(), reachableInitializer);
        rDeclarations.add(reachableDeclaration);
      }
    }
    if (pBitVectorVariables.isLastDenseBitVectorPresentByAccessType(pAccessType)) {
      LastDenseBitVector lastDenseBitVector =
          pBitVectorVariables.getLastDenseBitVectorByAccessType(pAccessType);
      // the last bv is initialized to 0, and assigned to something else in the last update later
      BitVectorValueExpression initializer =
          BitVectorUtil.buildBitVectorExpression(pOptions, pMemoryModel, ImmutableSet.of());
      // reachable last bit vector
      SeqBitVectorDeclaration reachableDeclaration =
          new SeqBitVectorDeclaration(type, lastDenseBitVector.reachableVariable, initializer);
      rDeclarations.add(reachableDeclaration);
    }
    return rDeclarations.build();
  }

  private static ImmutableSet<MemoryLocation> getDirectMemoryLocationsByAccessType(
      MemoryModel pMemoryModel,
      ImmutableList<SeqThreadStatementClause> pClauses,
      MemoryAccessType pAccessType) {

    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToBlock(pClauses);
    SeqThreadStatementBlock firstBlock = pClauses.getFirst().getFirstBlock();
    return MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
        labelBlockMap, firstBlock, pMemoryModel, pAccessType);
  }

  private static ImmutableSet<MemoryLocation> getReachableMemoryLocationsByAccessType(
      MemoryModel pMemoryModel,
      ImmutableList<SeqThreadStatementClause> pClauses,
      MemoryAccessType pAccessType) {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToBlock(pClauses);
    SeqThreadStatementBlock firstBlock = pClauses.getFirst().getFirstBlock();
    return MemoryLocationFinder.findReachableMemoryLocationsByAccessType(
        labelClauseMap, labelBlockMap, firstBlock, pMemoryModel, pAccessType);
  }

  // SPARSE ========================================================================================

  private static ImmutableList<SeqBitVectorDeclaration> buildSparseBitVectorDeclarationsByReduction(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    return switch (pOptions.reductionMode) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          buildSparseBitVectorDeclarations(
              pMemoryModel, pBitVectorVariables, pClauses, MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          // TODO add direct READ here for K == 0 reduction
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(
                  buildSparseBitVectorDeclarations(
                      pMemoryModel, pBitVectorVariables, pClauses, MemoryAccessType.ACCESS))
              .addAll(
                  buildSparseBitVectorDeclarations(
                      pMemoryModel, pBitVectorVariables, pClauses, MemoryAccessType.WRITE))
              .build();
    };
  }

  // TODO split into separate functions
  private static ImmutableList<SeqBitVectorDeclaration> buildSparseBitVectorDeclarations(
      MemoryModel pMemoryModel,
      BitVectorVariables pBitVectorVariables,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      MemoryAccessType pAccessType) {

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    ImmutableMap<MemoryLocation, SparseBitVector> sparseBitVectors =
        pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType);
    for (MPORThread thread : pClauses.keySet()) {
      ImmutableSet<MemoryLocation> reachableMemoryLocations =
          getReachableMemoryLocationsByAccessType(pMemoryModel, pClauses.get(thread), pAccessType);
      for (var entry : sparseBitVectors.entrySet()) {
        assert entry.getValue().variables.containsKey(thread) : "thread must have sparse variable";
        CIdExpression variable = entry.getValue().variables.get(thread);
        final boolean value = reachableMemoryLocations.contains(entry.getKey());
        SparseBitVectorValueExpression initializer = new SparseBitVectorValueExpression(value);
        SeqBitVectorDeclaration declaration =
            new SeqBitVectorDeclaration(BitVectorDataType.__UINT8_T, variable, initializer);
        rDeclarations.add(declaration);
      }
    }
    Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>> lastSparseBitVectors =
        pBitVectorVariables.tryGetLastSparseBitVectorByAccessType(pAccessType);
    if (lastSparseBitVectors.isPresent()) {
      for (LastSparseBitVector sparseBitVector : lastSparseBitVectors.orElseThrow().values()) {
        // last is initialized to 0, and assigned to something else in the last update later
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
