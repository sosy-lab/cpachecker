// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.declaration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorVariables.DenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorVariables.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorVariables.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorVariables.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CBitVectorLiteralExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;

public record SeqBitVectorDeclarationBuilder(
    BitVectorEncoding bitVectorEncoding,
    boolean reduceIgnoreSleep,
    ReductionMode reductionMode,
    BitVectorVariables bitVectorVariables,
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses,
    MemoryModel memoryModel) {

  /**
   * Returns, if enabled, the list of bit vector declarations based on {@code pOptions}. Note that
   * bit vectors are always initialized with {@code 0} for all indices, the actual assignment based
   * on the threads first statement is done when a thread is marked as active / created.
   */
  public ImmutableList<SeqBitVectorDeclaration> buildBitVectorDeclarationsByEncoding() {
    return switch (bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build bit vector declarations for encoding " + bitVectorEncoding);
      case BINARY, DECIMAL, HEXADECIMAL -> buildDenseBitVectorDeclarationsByReductionMode();
      case SPARSE -> buildSparseBitVectorDeclarationsByReductionMode();
    };
  }

  // DENSE =========================================================================================

  private ImmutableList<SeqBitVectorDeclaration> buildDenseBitVectorDeclarationsByReductionMode() {
    return switch (reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build bit vector declarations for reductionMode " + reductionMode);
      case ACCESS_ONLY -> buildDenseBitVectorDeclarationsByAccessType(MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(buildDenseBitVectorDeclarationsByAccessType(MemoryAccessType.ACCESS))
              .addAll(buildDenseBitVectorDeclarationsByAccessType(MemoryAccessType.READ))
              .addAll(buildDenseBitVectorDeclarationsByAccessType(MemoryAccessType.WRITE))
              .build();
    };
  }

  private ImmutableList<SeqBitVectorDeclaration> buildDenseBitVectorDeclarationsByAccessType(
      MemoryAccessType pAccessType) {

    int binaryLength = BitVectorUtil.getBinaryLength(memoryModel);
    BitVectorDataType dataType = BitVectorUtil.getDataTypeByLength(binaryLength);
    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    rDeclarations.addAll(buildCurrentDenseBitVectorDeclarations(dataType, pAccessType));
    tryBuildLastDenseBitVectorDeclaration(dataType, pAccessType).ifPresent(rDeclarations::add);
    return rDeclarations.build();
  }

  private ImmutableList<SeqBitVectorDeclaration> buildCurrentDenseBitVectorDeclarations(
      BitVectorDataType pDataType, MemoryAccessType pAccessType) {

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();

    ImmutableSet<DenseBitVector> denseBitVectors =
        bitVectorVariables.getDenseBitVectorsByAccessType(pAccessType);
    for (DenseBitVector denseBitVector : denseBitVectors) {
      MPORThread thread = denseBitVector.thread();

      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses.get(thread));
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToBlock(clauses.get(thread));
      SeqThreadStatementBlock firstBlock = clauses.get(thread).getFirst().getFirstBlock();

      for (ReachType reachType : ReachType.values()) {
        if (BitVectorUtil.isAccessReachPairNeeded(
            reduceIgnoreSleep, reductionMode, pAccessType, reachType)) {
          ImmutableSet<SeqMemoryLocation> memoryLocations =
              SeqMemoryLocationFinder.findMemoryLocationsByReachType(
                  labelClauseMap, labelBlockMap, firstBlock, memoryModel, pAccessType, reachType);
          CBitVectorLiteralExpression initializer =
              BitVectorUtil.buildBitVectorExpression(
                  bitVectorEncoding, memoryModel, memoryLocations);
          rDeclarations.add(
              new SeqBitVectorDeclaration(
                  pDataType, denseBitVector.getVariableByReachType(reachType), initializer));
        }
      }
    }
    return rDeclarations.build();
  }

  private Optional<SeqBitVectorDeclaration> tryBuildLastDenseBitVectorDeclaration(
      BitVectorDataType pDataType, MemoryAccessType pAccessType) {

    if (bitVectorVariables.isLastDenseBitVectorPresentByAccessType(pAccessType)) {
      LastDenseBitVector lastDenseBitVector =
          bitVectorVariables.getLastDenseBitVectorByAccessType(pAccessType);
      // the last bv is initialized to 0, and assigned to something else in the last update later
      CBitVectorLiteralExpression initializer =
          BitVectorUtil.buildBitVectorExpression(bitVectorEncoding, memoryModel, ImmutableSet.of());
      // reachable last bit vector
      return Optional.of(
          new SeqBitVectorDeclaration(
              pDataType, lastDenseBitVector.reachableVariable(), initializer));
    }
    return Optional.empty();
  }

  // SPARSE ========================================================================================

  private ImmutableList<SeqBitVectorDeclaration> buildSparseBitVectorDeclarationsByReductionMode() {
    return switch (reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build bit vector declarations for reductionMode " + reductionMode);
      case ACCESS_ONLY -> buildSparseBitVectorDeclarations(MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<SeqBitVectorDeclaration>builder()
              .addAll(buildSparseBitVectorDeclarations(MemoryAccessType.ACCESS))
              .addAll(buildSparseBitVectorDeclarations(MemoryAccessType.READ))
              .addAll(buildSparseBitVectorDeclarations(MemoryAccessType.WRITE))
              .build();
    };
  }

  private ImmutableList<SeqBitVectorDeclaration> buildSparseBitVectorDeclarations(
      MemoryAccessType pAccessType) {

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    // first add declarations for the current bitvectors
    for (MPORThread thread : clauses.keySet()) {
      rDeclarations.addAll(
          buildCurrentSparseBitVectorDeclarations(
              thread,
              bitVectorVariables.getSparseBitVectorByAccessType(pAccessType),
              clauses.get(thread),
              pAccessType));
    }
    rDeclarations.addAll(buildLastSparseBitVectorDeclaration(pAccessType));
    return rDeclarations.build();
  }

  private ImmutableList<SeqBitVectorDeclaration> buildCurrentSparseBitVectorDeclarations(
      MPORThread pThread,
      ImmutableMap<SeqMemoryLocation, SparseBitVector> pSparseBitVectors,
      ImmutableList<SeqThreadStatementClause> pClauses,
      MemoryAccessType pAccessType) {

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToBlock(pClauses);
    SeqThreadStatementBlock firstBlock = pClauses.getFirst().getFirstBlock();

    for (ReachType reachType : ReachType.values()) {
      ImmutableSet<SeqMemoryLocation> memoryLocations =
          SeqMemoryLocationFinder.findMemoryLocationsByReachType(
              labelClauseMap, labelBlockMap, firstBlock, memoryModel, pAccessType, reachType);
      for (var entry : pSparseBitVectors.entrySet()) {
        SeqMemoryLocation memoryLocation = entry.getKey();
        SparseBitVector sparseBitVector = entry.getValue();
        if (sparseBitVector.getVariablesByReachType(reachType).containsKey(pThread)) {
          CIdExpression idExpression =
              Objects.requireNonNull(
                  sparseBitVector.getVariablesByReachType(reachType).get(pThread));
          rDeclarations.add(
              buildSparseBitVectorDeclaration(
                  idExpression, memoryLocations.contains(memoryLocation)));
        }
      }
    }
    return rDeclarations.build();
  }

  private ImmutableList<SeqBitVectorDeclaration> buildLastSparseBitVectorDeclaration(
      MemoryAccessType pAccessType) {

    ImmutableList.Builder<SeqBitVectorDeclaration> rDeclarations = ImmutableList.builder();
    // then handle the declarations for the last bit vectors, if present
    Optional<ImmutableMap<SeqMemoryLocation, LastSparseBitVector>> lastSparseBitVectors =
        bitVectorVariables.tryGetLastSparseBitVectorByAccessType(pAccessType);
    if (lastSparseBitVectors.isPresent()) {
      for (LastSparseBitVector sparseBitVector : lastSparseBitVectors.orElseThrow().values()) {
        // last is initialized to 0, and assigned to something else in the last update later
        rDeclarations.add(
            new SeqBitVectorDeclaration(
                BitVectorDataType.UINT8_T,
                sparseBitVector.reachableVariable(),
                new CExpressionWrapper(CIntegerLiteralExpression.ZERO)));
      }
    }
    return rDeclarations.build();
  }

  private SeqBitVectorDeclaration buildSparseBitVectorDeclaration(
      CIdExpression pVariable, boolean pValue) {

    CExportExpression initializer =
        new CExpressionWrapper(
            pValue ? CIntegerLiteralExpression.ONE : CIntegerLiteralExpression.ZERO);
    return new SeqBitVectorDeclaration(BitVectorDataType.UINT8_T, pVariable, initializer);
  }
}
