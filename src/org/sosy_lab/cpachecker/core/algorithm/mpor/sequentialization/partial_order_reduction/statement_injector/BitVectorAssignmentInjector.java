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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingMap;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.PartialOrderReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record BitVectorAssignmentInjector(
    MPOROptions options,
    MPORThread activeThread,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap,
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap,
    SeqBitVectorVariables bitVectorVariables,
    MachineModel machineModel,
    SeqPointerAliasingMap pointerAliasingMap) {

  SeqThreadStatement injectBitVectorAssignmentsIntoStatement(SeqThreadStatement pStatement)
      throws UnsupportedCodeException {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pStatement.targetPc().isPresent()) {
      ImmutableList.Builder<SeqInstrumentation> newInstrumentation = ImmutableList.builder();
      int targetPc = pStatement.targetPc().orElseThrow();
      if (targetPc == ProgramCounterVariables.EXIT_PC) {
        // for the exit pc, reset the bit vector to just 0s
        newInstrumentation.addAll(buildBitVectorResets());
        return SeqThreadStatementUtil.appendedInstrumentationStatement(
            pStatement, newInstrumentation.build());

      } else {
        // for all other target pc, set the bit vector based on global accesses in the target block
        SeqThreadStatementClause newTarget = Objects.requireNonNull(labelClauseMap.get(targetPc));
        // the assignment is injected after the evaluation, it is only needed when commute fails
        newInstrumentation.addAll(buildBitVectorAssignmentsByReduction(newTarget));
        return SeqThreadStatementUtil.appendedInstrumentationStatement(
            pStatement, newInstrumentation.build());
      }
    }
    // no injection possible -> return statement as is
    return pStatement;
  }

  // Bit Vector Resets =============================================================================

  private ImmutableList<SeqInstrumentation> buildBitVectorResets() throws UnsupportedCodeException {
    checkArgument(
        !options.partialOrderReductionMode().equals(PartialOrderReductionMode.NONE),
        "cannot build assignments for reduction NONE");

    ImmutableList.Builder<SeqInstrumentation> rAssignments = ImmutableList.builder();
    for (SeqMemoryAccessType accessType : SeqMemoryAccessType.values()) {
      for (SeqMemoryReachType reachType : SeqMemoryReachType.values()) {
        if (SeqBitVectorUtil.isAccessReachPairNeeded(options, accessType, reachType)) {
          rAssignments.addAll(
              buildBitVectorAssignmentByEncoding(ImmutableSet.of(), accessType, reachType));
        }
      }
    }
    return rAssignments.build();
  }

  // Bit Vector Assignments ========================================================================

  private ImmutableList<SeqInstrumentation> buildBitVectorAssignmentsByReduction(
      SeqThreadStatementClause pTargetClause) throws UnsupportedCodeException {

    checkArgument(
        !options.partialOrderReductionMode().equals(PartialOrderReductionMode.NONE),
        "cannot build assignments for reduction NONE");

    ImmutableList.Builder<SeqInstrumentation> rAssignments = ImmutableList.builder();
    for (SeqMemoryAccessType accessType : SeqMemoryAccessType.values()) {
      for (SeqMemoryReachType reachType : SeqMemoryReachType.values()) {
        if (SeqBitVectorUtil.isAccessReachPairNeeded(options, accessType, reachType)) {
          ImmutableSet<SeqMemoryLocation> memoryLocations =
              SeqMemoryLocationFinder.findMemoryLocationsByReachType(
                  labelClauseMap,
                  labelBlockMap,
                  pTargetClause.getFirstBlock(),
                  pointerAliasingMap,
                  accessType,
                  reachType);
          rAssignments.addAll(
              buildBitVectorAssignmentByEncoding(memoryLocations, accessType, reachType));
        }
      }
    }
    return rAssignments.build();
  }

  private ImmutableList<SeqInstrumentation> buildBitVectorAssignmentByEncoding(
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      SeqMemoryAccessType pAccessType,
      SeqMemoryReachType pReachType)
      throws UnsupportedCodeException {

    return switch (options.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build bit vector assignments for encoding NONE");
      case BINARY, OCTAL, DECIMAL, HEXADECIMAL ->
          buildDenseBitVectorAssignment(pMemoryLocations, pAccessType, pReachType);
      case SPARSE -> buildSparseBitVectorAssignments(pMemoryLocations, pAccessType, pReachType);
    };
  }

  private ImmutableList<SeqInstrumentation> buildDenseBitVectorAssignment(
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      SeqMemoryAccessType pAccessType,
      SeqMemoryReachType pReachType)
      throws UnsupportedCodeException {

    if (!SeqBitVectorUtil.isAccessReachPairNeeded(options, pAccessType, pReachType)) {
      return ImmutableList.of();
    }
    CIdExpression bitVectorVariable =
        bitVectorVariables.getDenseBitVector(activeThread, pAccessType, pReachType);
    CIntegerLiteralExpression bitVectorExpression =
        SeqBitVectorUtil.buildBitVectorExpression(
            options.bitVectorEncoding(), machineModel, pointerAliasingMap, pMemoryLocations);
    return ImmutableList.of(
        SeqInstrumentationBuilder.buildBitVectorUpdateStatement(
            bitVectorVariable, bitVectorExpression));
  }

  private ImmutableList<SeqInstrumentation> buildSparseBitVectorAssignments(
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      SeqMemoryAccessType pAccessType,
      SeqMemoryReachType pReachType) {

    if (!SeqBitVectorUtil.isAccessReachPairNeeded(options, pAccessType, pReachType)) {
      return ImmutableList.of();
    }
    // use list so that the assignment order is deterministic
    ImmutableList.Builder<SeqInstrumentation> rAssignments = ImmutableList.builder();
    for (var entry : bitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      Optional<CIdExpression> sparseVariable =
          entry.getValue().tryGetVariableByReachTypeAndThread(pReachType, activeThread);
      if (sparseVariable.isPresent()) {
        Optional<SeqInstrumentation> assignment =
            tryBuildSparseBitVectorAssignmentByReachType(
                entry.getKey(), sparseVariable.orElseThrow(), pMemoryLocations, pReachType);
        if (assignment.isPresent()) {
          rAssignments.add(assignment.orElseThrow());
        }
      }
    }
    return rAssignments.build();
  }

  private Optional<SeqInstrumentation> tryBuildSparseBitVectorAssignmentByReachType(
      SeqMemoryLocation pMemoryLocation,
      CIdExpression pSparseVariable,
      ImmutableSet<SeqMemoryLocation> pMemoryLocations,
      SeqMemoryReachType pReachType) {

    // If 'pruneSparseBitVectorWrites' is enabled, then all sparse reachable bit vectors that are
    // written to 1 (i.e., if rightHandSide is true) such as 'reach = 1;' are pruned.
    // Pruning the write is sound because 'reach' is initialized to 1 anyway and does not have to be
    // reassigned its initial value.
    // Later, at some location during the thread simulation, 'reach' is set to 0 because the
    // respective memory location is not reachable anymore from that location onward.
    boolean rightHandSide = pMemoryLocations.contains(pMemoryLocation);
    if (options.pruneSparseBitVectorWrites()
        && rightHandSide
        && pReachType.equals(SeqMemoryReachType.REACHABLE)) {
      return Optional.empty();
    }

    CIntegerLiteralExpression sparseBitVectorExpression =
        rightHandSide ? CIntegerLiteralExpression.ONE : CIntegerLiteralExpression.ZERO;
    return Optional.of(
        SeqInstrumentationBuilder.buildBitVectorUpdateStatement(
            pSparseVariable, sparseBitVectorExpression));
  }
}
