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
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocationUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqBitVectorDeclarationBuilder {

  /**
   * Returns, if enabled, the list of bit vector declarations based on {@code pOptions}. Note that
   * bit vectors are always initialized with {@code 0} for all indices, the actual assignment based
   * on the threads first statement is done when a thread is marked as active / created.
   */
  public static ImmutableList<SeqBitVectorDeclaration> buildBitVectorDeclarationsByEncoding(
      MPOROptions pOptions, SequentializationFields pFields) {

    if (!pOptions.areBitVectorsEnabled()) {
      return ImmutableList.of();
    }
    return switch (pOptions.bitVectorEncoding) {
      case NONE -> ImmutableList.of();
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildDenseBitVectorDeclarationsByReduction(pOptions, pFields);
      case SPARSE -> buildSparseBitVectorDeclarationsByReduction(pOptions, pFields);
    };
  }

  // DENSE =========================================================================================

  private static ImmutableList<SeqBitVectorDeclaration> buildDenseBitVectorDeclarationsByReduction(
      MPOROptions pOptions, SequentializationFields pFields) {

    return switch (pOptions.reductionMode) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY ->
          buildDenseBitVectorDeclarationsByAccessType(pOptions, pFields, MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(
                  buildDenseBitVectorDeclarationsByAccessType(
                      pOptions, pFields, MemoryAccessType.ACCESS))
              .addAll(
                  buildDenseBitVectorDeclarationsByAccessType(
                      pOptions, pFields, MemoryAccessType.READ))
              .addAll(
                  buildDenseBitVectorDeclarationsByAccessType(
                      pOptions, pFields, MemoryAccessType.WRITE))
              .build();
    };
  }

  // TODO split into separate functions
  private static ImmutableList<SeqBitVectorDeclaration> buildDenseBitVectorDeclarationsByAccessType(
      MPOROptions pOptions, SequentializationFields pFields, MemoryAccessType pAccessType) {

    BitVectorVariables bitVectorVariables =
        pFields.ghostElements.getBitVectorVariables().orElseThrow();
    MemoryModel memoryModel = pFields.memoryModel.orElseThrow();
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses = pFields.clauses;

    int binaryLength = BitVectorUtil.getBinaryLength(memoryModel);
    BitVectorDataType type = BitVectorUtil.getDataTypeByLength(binaryLength);
    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (DenseBitVector denseBitVector :
        bitVectorVariables.getDenseBitVectorsByAccessType(pAccessType)) {
      MPORThread thread = denseBitVector.getThread();
      if (pOptions.reduceIgnoreSleep && denseBitVector.isDirectVariablePresent()) {
        ImmutableSet<MemoryLocation> directMemoryLocations =
            MemoryLocationUtil.getDirectMemoryLocationsByAccessType(
                memoryModel, clauses.get(thread).getFirst(), clauses.get(thread), pAccessType);
        BitVectorValueExpression directInitializer =
            BitVectorUtil.buildBitVectorExpression(pOptions, memoryModel, directMemoryLocations);
        // direct bit vector
        SeqBitVectorDeclaration directDeclaration =
            new SeqBitVectorDeclaration(
                type, denseBitVector.getDirectVariable(), directInitializer);
        rDeclarations.add(directDeclaration);
      }
      if (denseBitVector.isReachableVariablePresent()) {
        // TODO we can optimize here by saving the 0 initializers and leaving them out entirely
        //  or not write them ever again
        ImmutableSet<MemoryLocation> reachableMemoryLocations =
            MemoryLocationUtil.getReachableMemoryLocationsByAccessType(
                memoryModel, clauses.get(thread), pAccessType);
        BitVectorValueExpression reachableInitializer =
            BitVectorUtil.buildBitVectorExpression(pOptions, memoryModel, reachableMemoryLocations);
        // reachable bit vector
        SeqBitVectorDeclaration reachableDeclaration =
            new SeqBitVectorDeclaration(
                type, denseBitVector.getReachableVariable(), reachableInitializer);
        rDeclarations.add(reachableDeclaration);
      }
    }
    if (bitVectorVariables.isLastDenseBitVectorPresentByAccessType(pAccessType)) {
      LastDenseBitVector lastDenseBitVector =
          bitVectorVariables.getLastDenseBitVectorByAccessType(pAccessType);
      // the last bv is initialized to 0, and assigned to something else in the last update later
      BitVectorValueExpression initializer =
          BitVectorUtil.buildBitVectorExpression(pOptions, memoryModel, ImmutableSet.of());
      // reachable last bit vector
      SeqBitVectorDeclaration reachableDeclaration =
          new SeqBitVectorDeclaration(type, lastDenseBitVector.reachableVariable, initializer);
      rDeclarations.add(reachableDeclaration);
    }
    return rDeclarations.build();
  }

  // SPARSE ========================================================================================

  private static ImmutableList<SeqBitVectorDeclaration> buildSparseBitVectorDeclarationsByReduction(
      MPOROptions pOptions, SequentializationFields pFields) {

    return switch (pOptions.reductionMode) {
      case NONE -> ImmutableList.of();
      case ACCESS_ONLY -> buildSparseBitVectorDeclarations(pFields, MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(buildSparseBitVectorDeclarations(pFields, MemoryAccessType.ACCESS))
              .addAll(buildSparseBitVectorDeclarations(pFields, MemoryAccessType.READ))
              .addAll(buildSparseBitVectorDeclarations(pFields, MemoryAccessType.WRITE))
              .build();
    };
  }

  // TODO split into separate functions
  private static ImmutableList<SeqBitVectorDeclaration> buildSparseBitVectorDeclarations(
      SequentializationFields pFields, MemoryAccessType pAccessType) {

    BitVectorVariables bitVectorVariables =
        pFields.ghostElements.getBitVectorVariables().orElseThrow();
    MemoryModel memoryModel = pFields.memoryModel.orElseThrow();
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses = pFields.clauses;

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    ImmutableMap<MemoryLocation, SparseBitVector> sparseBitVectors =
        bitVectorVariables.getSparseBitVectorByAccessType(pAccessType);
    for (MPORThread thread : pFields.clauses.keySet()) {
      for (var entry : sparseBitVectors.entrySet()) {
        rDeclarations.addAll(
            buildSparseBitVectorDeclarations(
                entry.getValue(),
                thread,
                entry.getKey(),
                MemoryLocationUtil.mapMemoryLocationsToReachType(
                    memoryModel,
                    clauses.get(thread).getFirst(),
                    clauses.get(thread),
                    pAccessType)));
      }
    }
    Optional<ImmutableMap<MemoryLocation, LastSparseBitVector>> lastSparseBitVectors =
        bitVectorVariables.tryGetLastSparseBitVectorByAccessType(pAccessType);
    if (lastSparseBitVectors.isPresent()) {
      for (LastSparseBitVector sparseBitVector : lastSparseBitVectors.orElseThrow().values()) {
        // last is initialized to 0, and assigned to something else in the last update later
        SparseBitVectorValueExpression initializer = new SparseBitVectorValueExpression(false);
        SeqBitVectorDeclaration declaration =
            new SeqBitVectorDeclaration(
                BitVectorDataType.__UINT8_T, sparseBitVector.reachableVariable, initializer);
        rDeclarations.add(declaration);
      }
    }
    return rDeclarations.build();
  }

  private static ImmutableList<SeqBitVectorDeclaration> buildSparseBitVectorDeclarations(
      SparseBitVector pSparseBitVector,
      MPORThread pThread,
      MemoryLocation pMemoryLocation,
      ImmutableSetMultimap<ReachType, MemoryLocation> pMemoryLocations) {

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    for (ReachType reachType : ReachType.values()) {
      if (pSparseBitVector.getVariablesByReachType(reachType).containsKey(pThread)) {
        SeqBitVectorDeclaration declaration =
            buildSparseBitVectorDeclaration(
                pSparseBitVector.getVariablesByReachType(reachType).get(pThread),
                pMemoryLocations.get(reachType).contains(pMemoryLocation));
        rDeclarations.add(declaration);
      }
    }
    return rDeclarations.build();
  }

  private static SeqBitVectorDeclaration buildSparseBitVectorDeclaration(
      CIdExpression pVariable, boolean pValue) {

    SparseBitVectorValueExpression initializer = new SparseBitVectorValueExpression(pValue);
    return new SeqBitVectorDeclaration(BitVectorDataType.__UINT8_T, pVariable, initializer);
  }
}
