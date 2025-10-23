// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.BitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.SparseBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorAssignmentInjector {

  static SeqThreadStatement injectBitVectorAssignmentsIntoStatement(
      MPOROptions pOptions,
      final MPORThread pActiveThread,
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      final BitVectorVariables pBitVectorVariables,
      final MemoryModel pMemoryModel) {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc == Sequentialization.EXIT_PC) {
        // for the exit pc, reset the bit vector to just 0s
        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorResets =
            buildBitVectorResets(pOptions, pActiveThread, pBitVectorVariables, pMemoryModel);
        newInjected.addAll(bitVectorResets);
        return pCurrentStatement.cloneAppendingInjectedStatements(newInjected.build());
      } else {
        // for all other target pc, set the bit vector based on global accesses in the target block
        SeqThreadStatementClause newTarget = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        // the assignment is injected after the evaluation, it is only needed when commute fails
        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorAssignments =
            buildBitVectorAssignmentsByReduction(
                pOptions,
                pActiveThread,
                newTarget,
                pLabelClauseMap,
                pLabelBlockMap,
                pBitVectorVariables,
                pMemoryModel);
        newInjected.addAll(bitVectorAssignments);
        return pCurrentStatement.cloneAppendingInjectedStatements(newInjected.build());
      }
    }
    // no injection possible -> return statement as is
    return pCurrentStatement;
  }

  // Bit Vector Resets =============================================================================

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorResets(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel) {

    checkArgument(
        !pOptions.reductionMode.equals(ReductionMode.NONE),
        "cannot build assignments for reduction NONE");

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rAssignments = ImmutableList.builder();
    for (MemoryAccessType accessType : MemoryAccessType.values()) {
      for (ReachType reachType : ReachType.values()) {
        if (BitVectorUtil.isAccessReachPairNeeded(pOptions, accessType, reachType)) {
          rAssignments.addAll(
              buildBitVectorAssignmentByEncoding(
                  pOptions,
                  pThread,
                  pBitVectorVariables,
                  pMemoryModel,
                  ImmutableSet.of(),
                  accessType,
                  reachType));
        }
      }
    }
    return rAssignments.build();
  }

  // Bit Vector Assignments ========================================================================

  private static ImmutableList<SeqBitVectorAssignmentStatement>
      buildBitVectorAssignmentsByReduction(
          MPOROptions pOptions,
          MPORThread pActiveThread,
          SeqThreadStatementClause pTargetClause,
          ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
          ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
          BitVectorVariables pBitVectorVariables,
          MemoryModel pMemoryModel) {

    checkArgument(
        !pOptions.reductionMode.equals(ReductionMode.NONE),
        "cannot build assignments for reduction NONE");

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rAssignments = ImmutableList.builder();
    for (MemoryAccessType accessType : MemoryAccessType.values()) {
      for (ReachType reachType : ReachType.values()) {
        if (BitVectorUtil.isAccessReachPairNeeded(pOptions, accessType, reachType)) {
          ImmutableSet<SeqMemoryLocation> memoryLocations =
              SeqMemoryLocationFinder.findMemoryLocationsByReachType(
                  pLabelClauseMap,
                  pLabelBlockMap,
                  pTargetClause.getFirstBlock(),
                  pMemoryModel,
                  accessType,
                  reachType);
          rAssignments.addAll(
              buildBitVectorAssignmentByEncoding(
                  pOptions,
                  pActiveThread,
                  pBitVectorVariables,
                  pMemoryModel,
                  memoryLocations,
                  accessType,
                  reachType));
        }
      }
    }
    return rAssignments.build();
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorAssignmentByEncoding(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build bit vector assignments for encoding NONE");
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildDenseBitVectorAssignment(
              pOptions,
              pThread,
              pBitVectorVariables,
              pMemoryModel,
              pMemoryLocations,
              pAccessType,
              pReachType);
      case SPARSE ->
          buildSparseBitVectorAssignments(
              pOptions, pThread, pBitVectorVariables, pMemoryLocations, pAccessType, pReachType);
    };
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildDenseBitVectorAssignment(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    if (!BitVectorUtil.isAccessReachPairNeeded(pOptions, pAccessType, pReachType)) {
      return ImmutableList.of();
    }
    CIdExpression bitVectorVariable =
        pBitVectorVariables.getDenseBitVector(pThread, pAccessType, pReachType);
    BitVectorValueExpression bitVectorExpression =
        BitVectorUtil.buildBitVectorExpression(pOptions, pMemoryModel, pMemoryLocations);
    return ImmutableList.of(
        new SeqBitVectorAssignmentStatement(bitVectorVariable, bitVectorExpression));
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildSparseBitVectorAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    if (!BitVectorUtil.isAccessReachPairNeeded(pOptions, pAccessType, pReachType)) {
      return ImmutableList.of();
    }
    // use list so that the assignment order is deterministic
    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rAssignments = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      ImmutableMap<MPORThread, CIdExpression> variables =
          entry.getValue().getVariablesByReachType(pReachType);
      Optional<SeqBitVectorAssignmentStatement> assignment =
          buildSparseBitVectorAssignment(
              pOptions, entry.getKey(), pMemoryLocations, variables.get(pThread));
      if (assignment.isPresent()) {
        rAssignments.add(assignment.orElseThrow());
      }
    }
    return rAssignments.build();
  }

  private static Optional<SeqBitVectorAssignmentStatement> buildSparseBitVectorAssignment(
      MPOROptions pOptions,
      SeqMemoryLocation pMemoryLocation,
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      CIdExpression pVariable) {

    if (pVariable == null) {
      return Optional.empty();
    }
    // if enabled, consider only 0 writes (the memory location is not reachable anymore)
    if (pOptions.pruneSparseBitVectorWrites && pMemoryLocations.contains(pMemoryLocation)) {
      return Optional.empty();
    }
    boolean value = pMemoryLocations.contains(pMemoryLocation);
    SparseBitVectorValueExpression sparseBitVectorExpression =
        new SparseBitVectorValueExpression(value);
    return Optional.of(new SeqBitVectorAssignmentStatement(pVariable, sparseBitVectorExpression));
  }
}
