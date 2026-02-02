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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqThreadSimulationFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterministicSimulationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModelBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitutionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdgeBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadBuilder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SequentializationFields {

  public final int numThreads;

  /** The list of threads in the program, including the main thread and all pthreads. */
  public final ImmutableList<MPORThread> threads;

  public final ImmutableList<AVariableDeclaration> allGlobalVariableDeclarations;

  /**
   * The list of thread specific variable declaration substitutions. The substitution for the main
   * thread (0) handles global variables.
   */
  public final ImmutableList<MPORSubstitution> substitutions;

  /** The {@link MPORSubstitution} of the main thread, containing global variable substitutes. */
  public final MPORSubstitution mainSubstitution;

  public final ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges;

  public final Optional<MemoryModel> memoryModel;

  public final GhostElements ghostElements;

  public final ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses;

  public final Optional<ImmutableList<SeqThreadSimulationFunction>> threadSimulationFunctions;

  // TODO split into separate function so that unit tests create only what they test
  SequentializationFields(MPOROptions pOptions, CFA pInputCfa, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    resetStaticFields();
    MPORThreadBuilder threadBuilder = new MPORThreadBuilder(pOptions, pInputCfa);
    threads = threadBuilder.extractThreadsFromCfa();
    numThreads = threads.size();
    allGlobalVariableDeclarations = CFAUtils.getGlobalVariableDeclarations(pInputCfa);

    MPORSubstitutionBuilder substitutionBuilder =
        new MPORSubstitutionBuilder(pOptions, allGlobalVariableDeclarations, threads, pUtils);
    substitutions = substitutionBuilder.buildSubstitutions();
    mainSubstitution = SubstituteUtil.extractMainThreadSubstitution(substitutions);
    substituteEdges = SubstituteEdgeBuilder.substituteEdges(pOptions, substitutions);

    MemoryModelBuilder memoryModelBuilder =
        new MemoryModelBuilder(
            pOptions,
            SubstituteUtil.getInitialMemoryLocations(substituteEdges.values()),
            substituteEdges.values());
    memoryModel = memoryModelBuilder.tryBuildMemoryModel();

    GhostElementBuilder ghostElementBuilder =
        new GhostElementBuilder(
            pOptions,
            threads,
            substitutions,
            substituteEdges,
            memoryModel,
            pUtils.binaryExpressionBuilder());
    ghostElements = ghostElementBuilder.buildGhostElements();

    SeqThreadStatementClauseBuilder clauseBuilder =
        new SeqThreadStatementClauseBuilder(
            pOptions, threads, substitutions, substituteEdges, memoryModel, ghostElements, pUtils);
    clauses = clauseBuilder.buildClauses();

    threadSimulationFunctions =
        pOptions.loopUnrolling()
            ? Optional.of(
                NondeterministicSimulationBuilder.buildThreadSimulationFunctions(
                    pOptions, memoryModel, ghostElements, clauses, pUtils))
            : Optional.empty();
  }

  /** Resets all static fields, e.g. used for IDs. This may be necessary for unit tests. */
  private static void resetStaticFields() {
    MPORThreadBuilder.resetThreadId();
    MPORThreadBuilder.resetPc();
    CFAEdgeForThread.resetId();
    CFANodeForThread.resetId();
  }
}
