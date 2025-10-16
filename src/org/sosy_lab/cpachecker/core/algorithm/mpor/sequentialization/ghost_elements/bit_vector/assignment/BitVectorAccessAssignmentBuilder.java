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
import com.google.common.collect.ImmutableSetMultimap;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocationUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorAccessAssignmentBuilder {

  public static ImmutableList<SeqBitVectorAssignmentStatement> buildAccessBitVectorAssignments(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      SeqThreadStatementClause pTargetClause,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel) {

    ImmutableSetMultimap<ReachType, MemoryLocation> memoryLocations =
        MemoryLocationUtil.mapMemoryLocationsToReachType(
            pMemoryModel, pTargetClause, pLabelClauseMap.values(), MemoryAccessType.ACCESS);
    return buildAccessBitVectorAssignments(
        pOptions, pActiveThread, pBitVectorVariables, pMemoryModel, memoryLocations);
  }

  public static ImmutableList<SeqBitVectorAssignmentStatement> buildAccessBitVectorAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableSetMultimap<ReachType, MemoryLocation> pMemoryLocations) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    for (ReachType reachType : ReachType.values()) {
      rStatements.addAll(
          BitVectorAssignmentUtil.buildBitVectorAssignmentByEncoding(
              pOptions,
              pThread,
              pBitVectorVariables,
              pMemoryModel,
              pMemoryLocations.get(reachType),
              MemoryAccessType.ACCESS,
              reachType));
    }
    return rStatements.build();
  }
}
