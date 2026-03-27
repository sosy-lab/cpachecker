// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqMainFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqThreadSimulationFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CReturnStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;

class NextThreadNondeterministicSimulation extends NondeterministicSimulation {

  NextThreadNondeterministicSimulation(
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

    return new CCompoundStatement(
        listAndElement(
            buildAllPrecedingStatements(pThread),
            buildSingleThreadMultiSelectionStatement(pThread)));
  }

  @Override
  public CCompoundStatement buildAllThreadSimulations() throws UnrecognizedCodeException {
    // the inner multi selection statements choose the next statement, e.g. "pc0 == 1"
    ImmutableMap<CExportExpression, CCompoundStatement> innerMultiSelectionStatements =
        buildInnerMultiSelectionStatements();
    // the outer multi selection statement chooses the thread, e.g. "next_thread == 0"
    CExportStatement outerMultiSelectionStatement =
        buildMultiSelectionStatementByEncoding(
            options.selectionEncodingForThreads(),
            SeqIdExpressions.NEXT_THREAD,
            innerMultiSelectionStatements,
            utils.binaryExpressionBuilder());
    return new CCompoundStatement(ImmutableList.of(outerMultiSelectionStatement));
  }

  private ImmutableMap<CExportExpression, CCompoundStatement> buildInnerMultiSelectionStatements()
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExportExpression, CCompoundStatement> rStatements =
        ImmutableMap.builder();
    for (MPORThread thread : clauses.keySet()) {
      CExpression clauseExpression =
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              options.selectionEncodingForThreads(),
              SeqIdExpressions.NEXT_THREAD,
              thread.id(),
              utils.binaryExpressionBuilder());
      ImmutableList<CCompoundStatementElement> statements =
          listAndElement(
              buildAllPrecedingStatements(thread),
              buildSingleThreadMultiSelectionStatement(thread));
      rStatements.put(new CExpressionWrapper(clauseExpression), new CCompoundStatement(statements));
    }
    return rStatements.buildOrThrow();
  }

  @Override
  public CCompoundStatement buildPrecedingStatements(MPORThread pThread)
      throws UnrecognizedCodeException {

    Optional<CExportStatement> pcStatement = tryBuildPcPrecedingStatement(pThread);
    Optional<ImmutableList<CExportStatement>> nextThreadStatements =
        tryBuildNextThreadPrecedingStatements(pThread);

    ImmutableList.Builder<CCompoundStatementElement> rStatements = ImmutableList.builder();
    nextThreadStatements.ifPresent(l -> rStatements.addAll(l));
    pcStatement.ifPresent(s -> rStatements.add(s));
    return new CCompoundStatement(rStatements.build());
  }

  /**
   * Returns the preceding statement for {@code pc} but only if {@link
   * MPOROptions#scalarProgramCounters()} is enabled. In that case, the statement needs to be placed
   * inside the simulation. For array {@code pc}, it is placed at the loop head already (see {@link
   * SeqMainFunctionBuilder}).
   */
  Optional<CExportStatement> tryBuildPcPrecedingStatement(MPORThread pThread) {
    if (!options.threadSimulationUnrolling() && !options.scalarProgramCounters()) {
      return Optional.empty();
    }
    return Optional.of(
        new CStatementWrapper(
            ghostElements
                .getPcVariables()
                .buildScalarProgramCounterUnequalExitPcAssumption(pThread)));
  }

  /**
   * Returns the preceding statements for {@code next_thread}, but only if {@link
   * MPOROptions#threadSimulationUnrolling()} is enabled since otherwise {@code next_thread} is
   * chosen at the loop head already {@link SeqThreadSimulationFunctionBuilder} and is not required
   * in the simulation itself.
   */
  Optional<ImmutableList<CExportStatement>> tryBuildNextThreadPrecedingStatements(
      MPORThread pThread) throws UnrecognizedCodeException {

    if (!options.threadSimulationUnrolling()) {
      return Optional.empty();
    }

    // next_thread = __VERIFIER_nondet_...()
    CFunctionCallAssignmentStatement nextThreadAssignment =
        VerifierNondetFunctionType.buildNondetIntegerAssignment(
            options, SeqIdExpressions.NEXT_THREAD);

    // if (next_thread != {thread_id}) { return; }
    CBinaryExpression nextThreadEqualsThreadId =
        utils
            .binaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.NEXT_THREAD,
                SeqExpressionBuilder.buildIntegerLiteralExpression(pThread.id()),
                BinaryOperator.NOT_EQUALS);
    CReturnStatementWrapper returnStatement = new CReturnStatementWrapper(Optional.empty());
    CIfStatement nextThreadIfStatement =
        new CIfStatement(
            new CExpressionWrapper(nextThreadEqualsThreadId),
            new CCompoundStatement(returnStatement));

    return Optional.of(
        ImmutableList.of(new CStatementWrapper(nextThreadAssignment), nextThreadIfStatement));
  }
}
