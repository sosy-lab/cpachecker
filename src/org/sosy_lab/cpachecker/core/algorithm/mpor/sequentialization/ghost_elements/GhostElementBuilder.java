// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariableBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlags;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlagsBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class GhostElementBuilder {

  public static GhostElements buildGhostElements(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<CFAEdgeForThread, SubstituteEdge> pSubstituteEdges,
      Optional<MemoryModel> pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CIdExpression numThreadsIdExpression =
        SeqExpressionBuilder.buildNumThreadsIdExpression(pThreads.size());
    Optional<BitVectorVariables> bitVectorVariables =
        BitVectorBuilder.buildBitVectorVariables(
            pOptions, pThreads, pSubstituteEdges, pMemoryModel);
    ImmutableMap<MPORThread, FunctionStatements> functionStatements =
        FunctionStatementBuilder.buildFunctionStatements(
            pThreads, pSubstitutions, pSubstituteEdges);
    ProgramCounterVariables programCounterVariables =
        ProgramCounterVariableBuilder.buildProgramCounterVariables(
            pOptions, pThreads.size(), pBinaryExpressionBuilder);
    ThreadSyncFlags threadSyncFlags =
        ThreadSyncFlagsBuilder.buildThreadSyncFlags(pOptions, pThreads, pBinaryExpressionBuilder);

    return new GhostElements(
        numThreadsIdExpression,
        bitVectorVariables,
        functionStatements,
        programCounterVariables,
        buildThreadLabels(pOptions, pThreads),
        threadSyncFlags);
  }

  private static ImmutableMap<MPORThread, SeqThreadLabelStatement> buildThreadLabels(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) {

    if (!pOptions.isThreadLabelRequired()) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<MPORThread, SeqThreadLabelStatement> rLabels = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      String name = SeqNameUtil.buildThreadPrefix(pOptions, thread.getId());
      rLabels.put(thread, new SeqThreadLabelStatement(name));
    }
    return rLabels.buildOrThrow();
  }
}
