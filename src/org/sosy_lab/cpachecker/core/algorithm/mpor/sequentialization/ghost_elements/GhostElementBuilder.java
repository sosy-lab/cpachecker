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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingMap;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariablesBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.SeqFunctionStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.SeqFunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariableBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlags;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlagsBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CLabelStatement;

public record GhostElementBuilder(
    MPOROptions options,
    ImmutableList<MPORThread> threads,
    ImmutableList<MPORSubstitution> substitutions,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges,
    SeqPointerAliasingMap pointerAliasingMap,
    CBinaryExpressionBuilder binaryExpressionBuilder) {

  public GhostElements buildGhostElements() throws UnrecognizedCodeException {
    Optional<SeqBitVectorVariables> bitVectorVariables =
        options.isAnyBitVectorReductionEnabled()
            ? new SeqBitVectorVariablesBuilder(
                    options, threads, substituteEdges, pointerAliasingMap)
                .buildBitVectorVariables()
            : Optional.empty();

    SeqFunctionStatementBuilder functionStatementBuilder =
        new SeqFunctionStatementBuilder(threads, substitutions, substituteEdges);
    ImmutableMap<MPORThread, SeqFunctionStatements> functionStatements =
        functionStatementBuilder.buildFunctionStatements();

    ProgramCounterVariableBuilder pcVariableBuilder =
        new ProgramCounterVariableBuilder(
            options.scalarProgramCounters(),
            options.nondeterminismSource(),
            threads.size(),
            binaryExpressionBuilder);
    ProgramCounterVariables programCounterVariables =
        pcVariableBuilder.buildProgramCounterVariables();

    ThreadSyncFlagsBuilder threadSyncFlagsBuilder = new ThreadSyncFlagsBuilder(options, threads);
    ThreadSyncFlags threadSyncFlags = threadSyncFlagsBuilder.buildThreadSyncFlags();

    return new GhostElements(
        bitVectorVariables,
        functionStatements,
        programCounterVariables,
        buildThreadLabels(),
        threadSyncFlags);
  }

  private ImmutableMap<MPORThread, CLabelStatement> buildThreadLabels() {
    if (!options.isThreadLabelRequired()) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<MPORThread, CLabelStatement> rLabels = ImmutableMap.builder();
    for (MPORThread thread : threads) {
      String name = SeqNameUtil.buildThreadPrefix(options, thread.id());
      rLabels.put(thread, new CLabelStatement(name));
    }
    return rLabels.buildOrThrow();
  }
}
