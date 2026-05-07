// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingMap;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.DenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.PrevDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.PrevSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record SeqBitVectorDeclarationBuilder(
    MPOROptions options,
    SeqBitVectorVariables bitVectorVariables,
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses,
    MachineModel machineModel,
    SeqPointerAliasingMap pointerAliasingMap) {

  /**
   * Returns, if enabled, the list of bit vector declarations based on {@code pOptions}. Note that
   * bit vectors are always initialized with {@code 0} for all indices, the actual assignment based
   * on the threads first statement is done when a thread is marked as active / created.
   */
  public ImmutableList<CVariableDeclaration> buildBitVectorDeclarationsByEncoding()
      throws UnsupportedCodeException {

    return switch (options.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build bit vector declarations for encoding " + options.bitVectorEncoding());
      case BINARY, OCTAL, DECIMAL, HEXADECIMAL -> buildDenseBitVectorDeclarationsByReductionMode();
      case SPARSE -> buildSparseBitVectorDeclarationsByReductionMode();
    };
  }

  // DENSE =========================================================================================

  private ImmutableList<CVariableDeclaration> buildDenseBitVectorDeclarationsByReductionMode()
      throws UnsupportedCodeException {

    return switch (options.partialOrderReductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build bit vector declarations for partialOrderReductionMode "
                  + options.partialOrderReductionMode());
      case ACCESS_ONLY -> buildDenseBitVectorDeclarationsByAccessType(SeqMemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CVariableDeclaration>builder()
              .addAll(buildDenseBitVectorDeclarationsByAccessType(SeqMemoryAccessType.ACCESS))
              .addAll(buildDenseBitVectorDeclarationsByAccessType(SeqMemoryAccessType.READ))
              .addAll(buildDenseBitVectorDeclarationsByAccessType(SeqMemoryAccessType.WRITE))
              .build();
    };
  }

  private ImmutableList<CVariableDeclaration> buildDenseBitVectorDeclarationsByAccessType(
      SeqMemoryAccessType pAccessType) throws UnsupportedCodeException {

    CSimpleType type = SeqBitVectorUtil.getBitVectorType(machineModel, pointerAliasingMap);
    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();
    rDeclarations.addAll(buildCurrentDenseBitVectorDeclarations(type, pAccessType));
    tryBuildPrevDenseBitVectorDeclaration(type, pAccessType).ifPresent(rDeclarations::add);
    return rDeclarations.build();
  }

  private ImmutableList<CVariableDeclaration> buildCurrentDenseBitVectorDeclarations(
      CSimpleType pType, SeqMemoryAccessType pAccessType) throws UnsupportedCodeException {

    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();

    ImmutableSet<DenseBitVector> denseBitVectors =
        bitVectorVariables.getDenseBitVectorsByAccessType(pAccessType);
    for (DenseBitVector denseBitVector : denseBitVectors) {
      MPORThread thread = denseBitVector.thread();

      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses.get(thread));
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToBlock(clauses.get(thread));
      SeqThreadStatementBlock firstBlock = clauses.get(thread).getFirst().getFirstBlock();

      for (SeqMemoryReachType reachType : SeqMemoryReachType.values()) {
        if (SeqBitVectorUtil.isAccessReachPairNeeded(options, pAccessType, reachType)) {
          ImmutableSet<SeqMemoryLocation> memoryLocations =
              SeqMemoryLocationFinder.findMemoryLocationsByReachType(
                  labelClauseMap,
                  labelBlockMap,
                  firstBlock,
                  pointerAliasingMap,
                  pAccessType,
                  reachType);
          CIntegerLiteralExpression initializer =
              SeqBitVectorUtil.buildBitVectorExpression(
                  options.bitVectorEncoding(), machineModel, pointerAliasingMap, memoryLocations);
          rDeclarations.add(
              SeqDeclarationBuilder.buildVariableDeclaration(
                  true,
                  pType,
                  denseBitVector.getVariableByReachType(reachType).getName(),
                  new CInitializerExpression(FileLocation.DUMMY, initializer)));
        }
      }
    }
    return rDeclarations.build();
  }

  private Optional<CVariableDeclaration> tryBuildPrevDenseBitVectorDeclaration(
      CSimpleType pType, SeqMemoryAccessType pAccessType) throws UnsupportedCodeException {

    if (bitVectorVariables.isPrevDenseBitVectorPresentByAccessType(pAccessType)) {
      PrevDenseBitVector prevDenseBitVector =
          bitVectorVariables.getPrevDenseBitVectorByAccessType(pAccessType);
      // the prev bv is initialized to 0, and assigned to something else in the prev update later
      CIntegerLiteralExpression initializer =
          SeqBitVectorUtil.buildBitVectorExpression(
              options.bitVectorEncoding(), machineModel, pointerAliasingMap, ImmutableSet.of());
      // reachable prev bit vector
      return Optional.of(
          SeqDeclarationBuilder.buildVariableDeclaration(
              true,
              pType,
              prevDenseBitVector.directVariable().getName(),
              new CInitializerExpression(FileLocation.DUMMY, initializer)));
    }
    return Optional.empty();
  }

  // SPARSE ========================================================================================

  private ImmutableList<CVariableDeclaration> buildSparseBitVectorDeclarationsByReductionMode() {
    return switch (options.partialOrderReductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build bit vector declarations for partialOrderReductionMode "
                  + options.partialOrderReductionMode());
      case ACCESS_ONLY -> buildSparseBitVectorDeclarations(SeqMemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CVariableDeclaration>builder()
              .addAll(buildSparseBitVectorDeclarations(SeqMemoryAccessType.ACCESS))
              .addAll(buildSparseBitVectorDeclarations(SeqMemoryAccessType.READ))
              .addAll(buildSparseBitVectorDeclarations(SeqMemoryAccessType.WRITE))
              .build();
    };
  }

  private ImmutableList<CVariableDeclaration> buildSparseBitVectorDeclarations(
      SeqMemoryAccessType pAccessType) {

    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();
    // first add declarations for the current bitvectors
    for (MPORThread thread : clauses.keySet()) {
      rDeclarations.addAll(
          buildCurrentSparseBitVectorDeclarations(
              thread,
              bitVectorVariables.getSparseBitVectorByAccessType(pAccessType),
              clauses.get(thread),
              pAccessType));
    }
    rDeclarations.addAll(buildPrevSparseBitVectorDeclaration(pAccessType));
    return rDeclarations.build();
  }

  private ImmutableList<CVariableDeclaration> buildCurrentSparseBitVectorDeclarations(
      MPORThread pThread,
      ImmutableMap<SeqMemoryLocation, SparseBitVector> pSparseBitVectors,
      ImmutableList<SeqThreadStatementClause> pClauses,
      SeqMemoryAccessType pAccessType) {

    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToBlock(pClauses);
    SeqThreadStatementBlock firstBlock = pClauses.getFirst().getFirstBlock();

    for (SeqMemoryReachType reachType : SeqMemoryReachType.getPossibleReachTypes(options)) {
      ImmutableSet<SeqMemoryLocation> memoryLocations =
          SeqMemoryLocationFinder.findMemoryLocationsByReachType(
              labelClauseMap,
              labelBlockMap,
              firstBlock,
              pointerAliasingMap,
              pAccessType,
              reachType);
      for (var entry : pSparseBitVectors.entrySet()) {
        Optional<CIdExpression> idExpression =
            entry.getValue().tryGetVariableByReachTypeAndThread(reachType, pThread);
        if (idExpression.isPresent()) {
          rDeclarations.add(
              buildSparseBitVectorDeclaration(
                  idExpression.orElseThrow(), memoryLocations.contains(entry.getKey())));
        }
      }
    }
    return rDeclarations.build();
  }

  private ImmutableList<CVariableDeclaration> buildPrevSparseBitVectorDeclaration(
      SeqMemoryAccessType pAccessType) {

    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();
    // then handle the declarations for the prev bit vectors, if present
    Optional<ImmutableMap<SeqMemoryLocation, PrevSparseBitVector>> prevSparseBitVectors =
        bitVectorVariables.tryGetPrevSparseBitVectorByAccessType(pAccessType);
    if (prevSparseBitVectors.isPresent()) {
      for (PrevSparseBitVector sparseBitVector : prevSparseBitVectors.orElseThrow().values()) {
        // prev is initialized to 0, and assigned to something else in the prev update later
        rDeclarations.add(
            SeqDeclarationBuilder.buildVariableDeclaration(
                true,
                CNumericTypes.UNSIGNED_CHAR,
                sparseBitVector.directVariable().getName(),
                new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO)));
      }
    }
    return rDeclarations.build();
  }

  private CVariableDeclaration buildSparseBitVectorDeclaration(
      CIdExpression pVariable, boolean pValue) {

    CInitializerExpression initializerExpression =
        new CInitializerExpression(
            FileLocation.DUMMY,
            pValue ? CIntegerLiteralExpression.ONE : CIntegerLiteralExpression.ZERO);
    return SeqDeclarationBuilder.buildVariableDeclaration(
        true, CNumericTypes.UNSIGNED_CHAR, pVariable.getName(), initializerExpression);
  }
}
