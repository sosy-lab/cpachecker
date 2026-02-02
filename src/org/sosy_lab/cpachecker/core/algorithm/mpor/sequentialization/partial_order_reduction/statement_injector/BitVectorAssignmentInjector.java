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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CBitVectorLiteralExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.SparseBitVectorValueExpression;

public record BitVectorAssignmentInjector(
    MPOROptions options,
    MPORThread activeThread,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap,
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap,
    BitVectorVariables bitVectorVariables,
    MemoryModel memoryModel) {

  CSeqThreadStatement injectBitVectorAssignmentsIntoStatement(CSeqThreadStatement pStatement) {
    // if valid target pc found, inject bit vector write and evaluation statements
    if (pStatement.getTargetPc().isPresent()) {
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc == ProgramCounterVariables.EXIT_PC) {
        // for the exit pc, reset the bit vector to just 0s
        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorResets = buildBitVectorResets();
        newInjected.addAll(bitVectorResets);
        return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
            pStatement, newInjected.build());

      } else {
        // for all other target pc, set the bit vector based on global accesses in the target block
        SeqThreadStatementClause newTarget = Objects.requireNonNull(labelClauseMap.get(targetPc));
        // the assignment is injected after the evaluation, it is only needed when commute fails
        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorAssignments =
            buildBitVectorAssignmentsByReduction(newTarget);
        newInjected.addAll(bitVectorAssignments);
        return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
            pStatement, newInjected.build());
      }
    }
    // no injection possible -> return statement as is
    return pStatement;
  }

  // Bit Vector Resets =============================================================================

  private ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorResets() {
    checkArgument(
        !options.reductionMode().equals(ReductionMode.NONE),
        "cannot build assignments for reduction NONE");

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rAssignments = ImmutableList.builder();
    for (MemoryAccessType accessType : MemoryAccessType.values()) {
      for (ReachType reachType : ReachType.values()) {
        if (BitVectorUtil.isAccessReachPairNeeded(
            options.reduceIgnoreSleep(), options.reductionMode(), accessType, reachType)) {
          rAssignments.addAll(
              buildBitVectorAssignmentByEncoding(ImmutableSet.of(), accessType, reachType));
        }
      }
    }
    return rAssignments.build();
  }

  // Bit Vector Assignments ========================================================================

  private ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorAssignmentsByReduction(
      SeqThreadStatementClause pTargetClause) {

    checkArgument(
        !options.reductionMode().equals(ReductionMode.NONE),
        "cannot build assignments for reduction NONE");

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rAssignments = ImmutableList.builder();
    for (MemoryAccessType accessType : MemoryAccessType.values()) {
      for (ReachType reachType : ReachType.values()) {
        if (BitVectorUtil.isAccessReachPairNeeded(
            options.reduceIgnoreSleep(), options.reductionMode(), accessType, reachType)) {
          ImmutableSet<SeqMemoryLocation> memoryLocations =
              SeqMemoryLocationFinder.findMemoryLocationsByReachType(
                  labelClauseMap,
                  labelBlockMap,
                  pTargetClause.getFirstBlock(),
                  memoryModel,
                  accessType,
                  reachType);
          rAssignments.addAll(
              buildBitVectorAssignmentByEncoding(memoryLocations, accessType, reachType));
        }
      }
    }
    return rAssignments.build();
  }

  private ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorAssignmentByEncoding(
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    return switch (options.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build bit vector assignments for encoding NONE");
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildDenseBitVectorAssignment(pMemoryLocations, pAccessType, pReachType);
      case SPARSE -> buildSparseBitVectorAssignments(pMemoryLocations, pAccessType, pReachType);
    };
  }

  private ImmutableList<SeqBitVectorAssignmentStatement> buildDenseBitVectorAssignment(
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    if (!BitVectorUtil.isAccessReachPairNeeded(
        options.reduceIgnoreSleep(), options.reductionMode(), pAccessType, pReachType)) {
      return ImmutableList.of();
    }
    CIdExpression bitVectorVariable =
        bitVectorVariables.getDenseBitVector(activeThread, pAccessType, pReachType);
    CBitVectorLiteralExpression bitVectorExpression =
        BitVectorUtil.buildBitVectorExpression(
            options.bitVectorEncoding(), memoryModel, pMemoryLocations);
    return ImmutableList.of(
        new SeqBitVectorAssignmentStatement(bitVectorVariable, bitVectorExpression));
  }

  private ImmutableList<SeqBitVectorAssignmentStatement> buildSparseBitVectorAssignments(
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    if (!BitVectorUtil.isAccessReachPairNeeded(
        options.reduceIgnoreSleep(), options.reductionMode(), pAccessType, pReachType)) {
      return ImmutableList.of();
    }
    // use list so that the assignment order is deterministic
    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rAssignments = ImmutableList.builder();
    for (var entry : bitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      ImmutableMap<MPORThread, CIdExpression> sparseVariables =
          entry.getValue().getVariablesByReachType(pReachType);
      Optional<SeqBitVectorAssignmentStatement> assignment =
          buildSparseBitVectorAssignmentByReachType(
              entry.getKey(), sparseVariables, pMemoryLocations, pReachType);
      if (assignment.isPresent()) {
        rAssignments.add(assignment.orElseThrow());
      }
    }
    return rAssignments.build();
  }

  private Optional<SeqBitVectorAssignmentStatement> buildSparseBitVectorAssignmentByReachType(
      SeqMemoryLocation pMemoryLocation,
      ImmutableMap<MPORThread, CIdExpression> pSparseVariables,
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      ReachType pReachType) {

    if (!pSparseVariables.containsKey(activeThread)) {
      return Optional.empty();
    }
    // If 'pruneSparseBitVectorWrites' is enabled, then all sparse reachable bit vectors that are
    // written to 1 (i.e., if rightHandSide is true) such as 'reach = 1;' are pruned.
    // Pruning the write is sound because 'reach' is initialized to 1 anyway and does not have to be
    // reassigned its initial value.
    // Later, at some location during the thread simulation, 'reach' is set to 0 because the
    // respective memory location is not reachable anymore from that location onward.
    boolean rightHandSide = pMemoryLocations.contains(pMemoryLocation);
    if (options.pruneSparseBitVectorWrites()
        && rightHandSide
        && pReachType.equals(ReachType.REACHABLE)) {
      return Optional.empty();
    }
    CIdExpression sparseVariable = Objects.requireNonNull(pSparseVariables.get(activeThread));
    SparseBitVectorValueExpression sparseBitVectorExpression =
        new SparseBitVectorValueExpression(rightHandSide);
    return Optional.of(
        new SeqBitVectorAssignmentStatement(sparseVariable, sparseBitVectorExpression));
  }
}
