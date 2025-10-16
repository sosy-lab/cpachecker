// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryLocationUtil {

  public static ImmutableSetMultimap<ReachType, MemoryLocation> mapMemoryLocationsToReachType(
      MemoryModel pMemoryModel,
      SeqThreadStatementClause pDirectClause,
      ImmutableCollection<SeqThreadStatementClause> pAllClauses,
      MemoryAccessType pAccessType) {

    ImmutableSetMultimap.Builder<ReachType, MemoryLocation> rMap = ImmutableSetMultimap.builder();
    for (ReachType reachType : ReachType.values()) {
      rMap.putAll(
          reachType,
          getMemoryLocationsByReachType(
              pMemoryModel, pDirectClause, pAllClauses, pAccessType, reachType));
    }
    return rMap.build();
  }

  private static ImmutableSet<MemoryLocation> getMemoryLocationsByReachType(
      MemoryModel pMemoryModel,
      SeqThreadStatementClause pDirectClause,
      ImmutableCollection<SeqThreadStatementClause> pAllClauses,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    return switch (pReachType) {
      case DIRECT ->
          getDirectMemoryLocationsByAccessType(
              pMemoryModel, pDirectClause, pAllClauses, pAccessType);
      case REACHABLE ->
          getReachableMemoryLocationsByAccessType(pMemoryModel, pAllClauses, pAccessType);
    };
  }

  public static ImmutableSet<MemoryLocation> getDirectMemoryLocationsByAccessType(
      MemoryModel pMemoryModel,
      SeqThreadStatementClause pDirectClause,
      ImmutableCollection<SeqThreadStatementClause> pClauses,
      MemoryAccessType pAccessType) {

    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToBlock(pClauses);
    SeqThreadStatementBlock firstBlock = pDirectClause.getFirstBlock();
    return MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
        labelBlockMap, firstBlock, pMemoryModel, pAccessType);
  }

  public static ImmutableSet<MemoryLocation> getReachableMemoryLocationsByAccessType(
      MemoryModel pMemoryModel,
      ImmutableCollection<SeqThreadStatementClause> pClauses,
      MemoryAccessType pAccessType) {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToBlock(pClauses);
    SeqThreadStatementClause firstClause =
        labelClauseMap.entrySet().stream().min(Map.Entry.comparingByKey()).orElseThrow().getValue();
    SeqThreadStatementBlock firstBlock = Objects.requireNonNull(firstClause).getFirstBlock();
    return MemoryLocationFinder.findReachableMemoryLocationsByAccessType(
        labelClauseMap, labelBlockMap, firstBlock, pMemoryModel, pAccessType);
  }

  public static boolean isMemoryLocationReachableByThread(
      MemoryLocation pMemoryLocation,
      MemoryModel pMemoryModel,
      MPORThread pThread,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      MemoryAccessType pAccessType) {

    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
      ImmutableSet<MemoryLocation> memoryLocations =
          MemoryLocationFinder.findMemoryLocationsBySubstituteEdge(
              substituteEdge, pMemoryModel, pAccessType);
      if (memoryLocations.contains(pMemoryLocation)) {
        return true;
      }
    }
    return false;
  }

  static boolean isExplicitGlobal(CSimpleDeclaration pDeclaration) {
    if (pDeclaration instanceof CVariableDeclaration variableDeclaration) {
      return variableDeclaration.isGlobal();
    }
    return false;
  }

  static boolean isConstCpaCheckerTmp(MemoryLocation pMemoryLocation) {
    if (pMemoryLocation.declaration instanceof CVariableDeclaration variableDeclaration) {
      return MPORUtil.isConstCpaCheckerTmp(variableDeclaration);
    }
    return false;
  }
}
