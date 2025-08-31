// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModelBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitutionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdgeBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SequentializationFields {

  public final int numThreads;

  /** The list of threads in the program, including the main thread and all pthreads. */
  public final ImmutableList<MPORThread> threads;

  /**
   * The list of thread specific variable declaration substitutions. The substitution for the main
   * thread (0) handles global variables.
   */
  public final ImmutableList<MPORSubstitution> substitutions;

  /** The {@link MPORSubstitution} of the main thread, containing global variable substitutes. */
  public final MPORSubstitution mainSubstitution;

  public final ImmutableMap<ThreadEdge, SubstituteEdge> substituteEdges;

  public final Optional<MemoryModel> memoryModel;

  public final GhostElements ghostElements;

  public final ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses;

  // TODO split into separate function so that unit tests create only what they test
  SequentializationFields(
      MPOROptions pOptions,
      CFA pCfa,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    threads = ThreadBuilder.createThreads(pOptions, pCfa);
    numThreads = threads.size();
    substitutions =
        MPORSubstitutionBuilder.buildSubstitutions(
            pOptions,
            CFAUtils.getGlobalVariableDeclarations(pCfa),
            threads,
            pBinaryExpressionBuilder,
            pLogger);
    mainSubstitution = SubstituteUtil.extractMainThreadSubstitution(substitutions);
    substituteEdges = SubstituteEdgeBuilder.substituteEdges(pOptions, substitutions);
    memoryModel =
        MemoryModelBuilder.tryBuildMemoryModel(
            pOptions,
            SubstituteUtil.getInitialMemoryLocations(substituteEdges.values()),
            substituteEdges.values());
    ghostElements =
        GhostElementBuilder.buildGhostElements(
            pOptions,
            threads,
            substitutions,
            substituteEdges,
            memoryModel,
            pBinaryExpressionBuilder);
    clauses =
        SeqThreadStatementClauseBuilder.buildClauses(
            pOptions,
            substitutions,
            substituteEdges,
            memoryModel,
            ghostElements,
            pBinaryExpressionBuilder,
            pLogger);
  }
}
