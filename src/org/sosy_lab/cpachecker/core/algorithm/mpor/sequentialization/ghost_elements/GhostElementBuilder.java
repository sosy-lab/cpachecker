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
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqThreadLabelStatement;
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

public record GhostElementBuilder(
    MPOROptions options,
    ImmutableList<MPORThread> pThreads,
    ImmutableList<MPORSubstitution> pSubstitutions,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> pSubstituteEdges,
    Optional<MemoryModel> pMemoryModel,
    CBinaryExpressionBuilder pBinaryExpressionBuilder) {

  public GhostElements buildGhostElements() throws UnrecognizedCodeException {
    Optional<BitVectorVariables> bitVectorVariables =
        options.isAnyBitVectorReductionEnabled()
            ? new BitVectorBuilder(options, pThreads, pSubstituteEdges, pMemoryModel.orElseThrow())
                .buildBitVectorVariables()
            : Optional.empty();

    FunctionStatementBuilder functionStatementBuilder =
        new FunctionStatementBuilder(pThreads, pSubstitutions, pSubstituteEdges);
    ImmutableMap<MPORThread, FunctionStatements> functionStatements =
        functionStatementBuilder.buildFunctionStatements();

    ProgramCounterVariableBuilder pcVariableBuilder =
        new ProgramCounterVariableBuilder(options, pThreads.size(), pBinaryExpressionBuilder);
    ProgramCounterVariables programCounterVariables =
        pcVariableBuilder.buildProgramCounterVariables();

    ThreadSyncFlagsBuilder threadSyncFlagsBuilder =
        new ThreadSyncFlagsBuilder(options, pThreads, pBinaryExpressionBuilder);
    ThreadSyncFlags threadSyncFlags = threadSyncFlagsBuilder.buildThreadSyncFlags();

    return new GhostElements(
        bitVectorVariables,
        functionStatements,
        programCounterVariables,
        buildThreadLabels(),
        threadSyncFlags);
  }

  private ImmutableMap<MPORThread, SeqThreadLabelStatement> buildThreadLabels() {
    if (!options.isThreadLabelRequired()) {
      return ImmutableMap.of();
    }
    Builder<MPORThread, SeqThreadLabelStatement> rLabels = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      String name = SeqNameUtil.buildThreadPrefix(options, thread.id());
      rLabels.put(thread, new SeqThreadLabelStatement(name));
    }
    return rLabels.buildOrThrow();
  }
}
