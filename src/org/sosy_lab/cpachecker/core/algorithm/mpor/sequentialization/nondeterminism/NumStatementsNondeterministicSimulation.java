// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector.ReduceIgnoreSleepInjector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLabelStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;

class NumStatementsNondeterministicSimulation extends NondeterministicSimulation {

  NumStatementsNondeterministicSimulation(
      MPOROptions pOptions,
      MachineModel pMachineModel,
      Optional<MemoryModel> pMemoryModel,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils) {

    super(pOptions, pMachineModel, pMemoryModel, pGhostElements, pClauses, pUtils);
  }

  @Override
  public CCompoundStatement buildSingleThreadSimulation(MPORThread pThread)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CCompoundStatementElement> rSimulation = ImmutableList.builder();

    // add "T{thread_id}: label", if present
    Optional<CLabelStatement> threadLabel =
        Optional.ofNullable(ghostElements.threadLabels().get(pThread));
    if (threadLabel.isPresent()) {
      rSimulation.add(threadLabel.orElseThrow());
    }

    // add "if (pc != 0 ...)" condition
    CBinaryExpression ifCondition =
        ghostElements.getPcVariables().getThreadActiveExpression(pThread.id());
    ImmutableList.Builder<CCompoundStatementElement> ifBlock = ImmutableList.builder();

    // add the round_max = nondet assignment for this thread
    ifBlock.add(
        new CStatementWrapper(
            VerifierNondetFunctionType.buildNondetIntegerAssignment(
                options, SeqIdExpressions.ROUND_MAX)));

    // add the ignore sleep instrumentation, if enabled
    if (options.reduceIgnoreSleep()) {
      ImmutableSet<MPORThread> otherThreads = MPORUtil.withoutElement(clauses.keySet(), pThread);
      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses.get(pThread));
      ReduceIgnoreSleepInjector reduceIgnoreSleepInjector =
          new ReduceIgnoreSleepInjector(
              options, pThread, otherThreads, labelClauseMap, ghostElements, utils);
      ifBlock.add(reduceIgnoreSleepInjector.buildIgnoreSleepInstrumentation());
    }

    // if (round_max > 0) ...
    ImmutableList.Builder<CCompoundStatementElement> innerIfBlock = ImmutableList.builder();
    CExpression roundMaxGreaterZero =
        utils
            .binaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.ROUND_MAX,
                SeqIntegerLiteralExpressions.INT_0,
                BinaryOperator.GREATER_THAN);
    CExportExpression innerIfCondition = new CExpressionWrapper(roundMaxGreaterZero);

    // add the thread simulation statements
    innerIfBlock.addAll(buildAllPrecedingStatements(pThread));
    innerIfBlock.add(buildSingleThreadMultiSelectionStatement(pThread));
    CIfStatement innerIfStatement =
        new CIfStatement(innerIfCondition, new CCompoundStatement(innerIfBlock.build()));

    ifBlock.add(innerIfStatement);
    CIfStatement ifStatement =
        new CIfStatement(
            new CExpressionWrapper(ifCondition), new CCompoundStatement(ifBlock.build()));

    return new CCompoundStatement(rSimulation.add(ifStatement).build());
  }

  @Override
  public CCompoundStatement buildAllThreadSimulations() throws UnrecognizedCodeException {
    ImmutableList.Builder<CCompoundStatementElement> rThreadSimulations = ImmutableList.builder();
    for (MPORThread thread : clauses.keySet()) {
      rThreadSimulations.add(buildSingleThreadSimulation(thread));
    }
    return new CCompoundStatement(rThreadSimulations.build());
  }

  @Override
  public CCompoundStatement buildPrecedingStatements(MPORThread pThread) {
    // assume("pc active") is not necessary since the simulation starts with 'if (pc* != 0)'
    CExpressionAssignmentStatement roundReset = NondeterministicSimulationBuilder.buildRoundReset();
    return new CCompoundStatement(
        ImmutableList.<CCompoundStatementElement>builder()
            .add(new CStatementWrapper(roundReset))
            .build());
  }
}
