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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorAccessAssignmentBuilder {

  public static ImmutableList<SeqBitVectorAssignmentStatement> buildAccessBitVectorAssignments(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      SeqThreadStatementBlock pTargetBlock,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel) {

    ImmutableSet<MemoryLocation> directMemoryLocations =
        MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
            pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.ACCESS);
    ImmutableSet<MemoryLocation> reachableMemoryLocations =
        MemoryLocationFinder.findReachableMemoryLocationsByAccessType(
            pLabelClauseMap, pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.ACCESS);
    return buildAccessBitVectorAssignments(
        pOptions,
        pActiveThread,
        pBitVectorVariables,
        pMemoryModel,
        directMemoryLocations,
        reachableMemoryLocations);
  }

  public static ImmutableList<SeqBitVectorAssignmentStatement> buildAccessBitVectorAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableSet<MemoryLocation> pDirectMemoryLocations,
      ImmutableSet<MemoryLocation> pReachableMemoryLocations) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    if (pOptions.bitVectorEncoding.equals(BitVectorEncoding.SPARSE)) {
      if (pOptions.reduceIgnoreSleep) {
        rStatements.addAll(
            BitVectorAssignmentUtil.buildSparseBitVectorAssignments(
                pOptions,
                pThread,
                pBitVectorVariables,
                pDirectMemoryLocations,
                MemoryAccessType.ACCESS,
                ReachType.DIRECT));
      }
      rStatements.addAll(
          BitVectorAssignmentUtil.buildSparseBitVectorAssignments(
              pOptions,
              pThread,
              pBitVectorVariables,
              pReachableMemoryLocations,
              MemoryAccessType.ACCESS,
              ReachType.REACHABLE));
    } else {
      if (pOptions.reduceIgnoreSleep) {
        rStatements.add(
            BitVectorAssignmentUtil.buildDenseBitVectorAssignment(
                pOptions,
                pThread,
                pBitVectorVariables,
                pMemoryModel,
                pDirectMemoryLocations,
                MemoryAccessType.ACCESS,
                ReachType.DIRECT));
      }
      rStatements.add(
          BitVectorAssignmentUtil.buildDenseBitVectorAssignment(
              pOptions,
              pThread,
              pBitVectorVariables,
              pMemoryModel,
              pReachableMemoryLocations,
              MemoryAccessType.ACCESS,
              ReachType.REACHABLE));
    }
    return rStatements.build();
  }
}
