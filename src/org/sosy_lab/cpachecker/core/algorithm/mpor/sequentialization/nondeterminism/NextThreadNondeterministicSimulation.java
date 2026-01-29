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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqMainFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;

class NextThreadNondeterministicSimulation extends NondeterministicSimulation {

  NextThreadNondeterministicSimulation(
      MPOROptions pOptions,
      Optional<MemoryModel> pMemoryModel,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils) {

    super(pOptions, pMemoryModel, pGhostElements, pClauses, pUtils);
  }

  @Override
  public ImmutableList<CExportStatement> buildSingleThreadSimulation(MPORThread pThread)
      throws UnrecognizedCodeException {
    // return the multi control statement, no adjustments needed for this type of nondeterminism
    return ImmutableList.of(buildSingleThreadMultiControlStatement(pThread));
  }

  @Override
  public ImmutableList<CExportStatement> buildAllThreadSimulations()
      throws UnrecognizedCodeException {

    // the inner multi control statements choose the next statement, e.g. "pc == 1"
    ImmutableMap<CExportExpression, SeqMultiControlStatement> innerMultiControlStatements =
        buildInnerMultiControlStatements();
    // the outer multi control statement chooses the thread, e.g. "next_thread == 0"
    SeqMultiControlStatement outerMultiControlStatement =
        SeqMultiControlStatement.buildMultiControlStatementByEncoding(
            options.controlEncodingThread(),
            SeqIdExpressions.NEXT_THREAD,
            // the outer multi control statement never has an assumption
            ImmutableList.of(),
            innerMultiControlStatements,
            utils.binaryExpressionBuilder());
    return ImmutableList.of(outerMultiControlStatement);
  }

  private ImmutableMap<CExportExpression, SeqMultiControlStatement>
      buildInnerMultiControlStatements() throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExportExpression, SeqMultiControlStatement> rStatements =
        ImmutableMap.builder();
    for (MPORThread thread : clauses.keySet()) {
      CExpression clauseExpression =
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              options.controlEncodingThread(),
              SeqIdExpressions.NEXT_THREAD,
              thread.id(),
              utils.binaryExpressionBuilder());
      rStatements.put(
          new CExpressionWrapper(clauseExpression), buildSingleThreadMultiControlStatement(thread));
    }
    return rStatements.buildOrThrow();
  }

  @Override
  public ImmutableList<CExportStatement> buildPrecedingStatements(MPORThread pThread)
      throws UnrecognizedCodeException {

    Optional<CFunctionCallStatement> pcUnequalExitAssumption =
        tryBuildPcUnequalExitAssumption(pThread);
    Optional<ImmutableList<CStatement>> nextThreadStatements =
        tryBuildNextThreadStatements(pThread);

    ImmutableList.Builder<CExportStatement> rStatements = ImmutableList.builder();
    pcUnequalExitAssumption.ifPresent(s -> rStatements.add(new CStatementWrapper(s)));
    nextThreadStatements.ifPresent(l -> l.forEach(s -> rStatements.add(new CStatementWrapper(s))));
    return rStatements.build();
  }

  /**
   * Returns the {@link CFunctionCallStatement} to {@code assume(pc{pThread.id} != 0);} if {@link
   * MPOROptions#scalarPc()} is enabled. In that case, the assumptions needs to be placed inside the
   * simulation. For array {@code pc}, it is placed at the loop head already (see {@link
   * SeqMainFunction}).
   */
  protected Optional<CFunctionCallStatement> tryBuildPcUnequalExitAssumption(MPORThread pThread) {
    return options.scalarPc()
        ? Optional.of(ghostElements.getPcVariables().buildScalarPcUnequalExitPcAssumption(pThread))
        : Optional.empty();
  }

  protected Optional<ImmutableList<CStatement>> tryBuildNextThreadStatements(MPORThread pThread)
      throws UnrecognizedCodeException {

    if (!options.loopUnrolling()) {
      // when loopUnrolling is disabled, the next_thread is chosen -> no assumption needed
      return Optional.empty();
    }
    // next_thread = __VERIFIER_nondet_...()
    CFunctionCallAssignmentStatement nextThreadAssignment =
        VerifierNondetFunctionType.buildNondetIntegerAssignment(
            options, SeqIdExpressions.NEXT_THREAD);
    // assume(next_thread == {thread_id})
    CBinaryExpression nextThreadEqualsThreadId =
        utils
            .binaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.NEXT_THREAD,
                SeqExpressionBuilder.buildIntegerLiteralExpression(pThread.id()),
                BinaryOperator.EQUALS);
    CFunctionCallStatement nextThreadAssumption =
        SeqAssumeFunction.buildAssumeFunctionCallStatement(nextThreadEqualsThreadId);
    return Optional.of(ImmutableList.of(nextThreadAssignment, nextThreadAssumption));
  }
}
