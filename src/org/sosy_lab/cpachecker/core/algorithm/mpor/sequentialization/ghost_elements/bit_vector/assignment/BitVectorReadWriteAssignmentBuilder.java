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
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorReadWriteAssignmentBuilder {

  public static ImmutableList<SeqBitVectorAssignmentStatement> buildReadWriteBitVectorAssignments(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      SeqThreadStatementBlock pTargetBlock,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel) {

    ImmutableSet<MemoryLocation> directReadMemoryLocations =
        MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
            pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.READ);
    ImmutableSet<MemoryLocation> reachableWriteMemoryLocations =
        MemoryLocationFinder.findReachableMemoryLocationsByAccessType(
            pLabelClauseMap, pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.WRITE);

    ImmutableSet<MemoryLocation> directWriteMemoryLocations =
        MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
            pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.WRITE);
    ImmutableSet<MemoryLocation> reachableReadMemoryLocations =
        MemoryLocationFinder.findReachableMemoryLocationsByAccessType(
            pLabelClauseMap, pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.READ);

    return buildReadWriteBitVectorAssignments(
        pOptions,
        pActiveThread,
        pBitVectorVariables,
        pMemoryModel,
        directReadMemoryLocations,
        reachableWriteMemoryLocations,
        directWriteMemoryLocations,
        // combine both read and write for access
        ImmutableSet.<MemoryLocation>builder()
            .addAll(reachableReadMemoryLocations)
            .addAll(reachableWriteMemoryLocations)
            .build());
  }

  // TODO split into several functions
  public static ImmutableList<SeqBitVectorAssignmentStatement> buildReadWriteBitVectorAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pReachableWriteMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      ImmutableSet<MemoryLocation> pReachableAccessMemoryLocations) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    if (pOptions.bitVectorEncoding.equals(BitVectorEncoding.SPARSE)) {
      if (pOptions.kIgnoreZeroReduction) {
        rStatements.addAll(
            BitVectorAssignmentUtil.buildSparseDirectBitVectorAssignmentsByAccessType(
                pOptions,
                pThread,
                pBitVectorVariables,
                pDirectReadMemoryLocations,
                MemoryAccessType.READ));
        rStatements.addAll(
            BitVectorAssignmentUtil.buildSparseDirectBitVectorAssignmentsByAccessType(
                pOptions,
                pThread,
                pBitVectorVariables,
                pDirectWriteMemoryLocations,
                MemoryAccessType.WRITE));
      }
      rStatements.addAll(
          BitVectorAssignmentUtil.buildSparseReachableBitVectorAssignmentsByAccessType(
              pOptions,
              pThread,
              pBitVectorVariables,
              pReachableAccessMemoryLocations,
              MemoryAccessType.ACCESS));
      rStatements.addAll(
          BitVectorAssignmentUtil.buildSparseReachableBitVectorAssignmentsByAccessType(
              pOptions,
              pThread,
              pBitVectorVariables,
              pReachableWriteMemoryLocations,
              MemoryAccessType.WRITE));
    } else {
      if (pOptions.kIgnoreZeroReduction) {
        rStatements.add(
            BitVectorAssignmentUtil.buildDenseDirectBitVectorAssignmentByAccessType(
                pOptions,
                pThread,
                pBitVectorVariables,
                pMemoryModel,
                pDirectReadMemoryLocations,
                MemoryAccessType.READ));
        rStatements.add(
            BitVectorAssignmentUtil.buildDenseDirectBitVectorAssignmentByAccessType(
                pOptions,
                pThread,
                pBitVectorVariables,
                pMemoryModel,
                pDirectWriteMemoryLocations,
                MemoryAccessType.WRITE));
      }
      rStatements.add(
          BitVectorAssignmentUtil.buildDenseReachableBitVectorAssignmentByAccessType(
              pOptions,
              pThread,
              pBitVectorVariables,
              pMemoryModel,
              pReachableAccessMemoryLocations,
              MemoryAccessType.ACCESS));
      rStatements.add(
          BitVectorAssignmentUtil.buildDenseReachableBitVectorAssignmentByAccessType(
              pOptions,
              pThread,
              pBitVectorVariables,
              pMemoryModel,
              pReachableWriteMemoryLocations,
              MemoryAccessType.WRITE));
    }
    return rStatements.build();
  }
}
