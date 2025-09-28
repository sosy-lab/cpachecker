// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.assignment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.BitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.SparseBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

class BitVectorAssignmentUtil {

  static SeqBitVectorAssignmentStatement buildDenseDirectBitVectorAssignmentByAccessType(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableSet<MemoryLocation> pDirectMemoryLocations,
      MemoryAccessType pAccessType) {

    CExpression bitVectorVariable =
        pBitVectorVariables.getDenseBitVector(pThread, pAccessType, ReachType.DIRECT);
    BitVectorValueExpression bitVectorExpression =
        BitVectorUtil.buildBitVectorExpression(pOptions, pMemoryModel, pDirectMemoryLocations);
    return new SeqBitVectorAssignmentStatement(bitVectorVariable, bitVectorExpression);
  }

  static SeqBitVectorAssignmentStatement buildDenseReachableBitVectorAssignmentByAccessType(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableSet<MemoryLocation> pReachableMemoryLocations,
      MemoryAccessType pAccessType) {

    CExpression bitVectorVariable =
        pBitVectorVariables.getDenseBitVector(pThread, pAccessType, ReachType.REACHABLE);
    BitVectorValueExpression bitVectorExpression =
        BitVectorUtil.buildBitVectorExpression(pOptions, pMemoryModel, pReachableMemoryLocations);
    return new SeqBitVectorAssignmentStatement(bitVectorVariable, bitVectorExpression);
  }

  static ImmutableList<SeqBitVectorAssignmentStatement> buildSparseBitVectorAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableSet<MemoryLocation> pDirectMemoryLocations,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    // use list so that the assignment order is deterministic
    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rAssignments = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      ImmutableMap<MPORThread, CIdExpression> variables =
          entry.getValue().getVariablesByReachType(pReachType);
      Optional<SeqBitVectorAssignmentStatement> assignment =
          buildSparseBitVectorAssignment(
              pOptions, entry.getKey(), pDirectMemoryLocations, variables.get(pThread));
      if (assignment.isPresent()) {
        rAssignments.add(assignment.orElseThrow());
      }
    }
    return rAssignments.build();
  }

  static Optional<SeqBitVectorAssignmentStatement> buildSparseBitVectorAssignment(
      MPOROptions pOptions,
      MemoryLocation pMemoryLocation,
      ImmutableSet<MemoryLocation> pReachableMemoryLocations,
      CIdExpression pVariable) {

    // if enabled, consider only 0 writes (the memory location is not reachable anymore)
    if (!pOptions.pruneBitVectorWrite || !pReachableMemoryLocations.contains(pMemoryLocation)) {
      boolean value = pReachableMemoryLocations.contains(pMemoryLocation);
      SparseBitVectorValueExpression sparseBitVectorExpression =
          new SparseBitVectorValueExpression(value);
      return Optional.of(new SeqBitVectorAssignmentStatement(pVariable, sparseBitVectorExpression));
    }
    return Optional.empty();
  }
}
