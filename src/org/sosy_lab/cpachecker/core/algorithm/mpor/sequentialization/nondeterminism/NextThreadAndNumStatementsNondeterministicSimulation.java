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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

class NextThreadAndNumStatementsNondeterministicSimulation
    extends NextThreadNondeterministicSimulation {

  NextThreadAndNumStatementsNondeterministicSimulation(
      MPOROptions pOptions,
      Optional<MemoryModel> pMemoryModel,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils) {

    super(pOptions, pMemoryModel, pGhostElements, pClauses, pUtils);
  }

  @Override
  public ImmutableList<CExportStatement> buildPrecedingStatements(MPORThread pThread)
      throws UnrecognizedCodeException {

    Optional<CFunctionCallStatement> pcUnequalExitAssumption =
        tryBuildPcUnequalExitAssumption(pThread);
    Optional<ImmutableList<CStatement>> nextThreadStatements =
        tryBuildNextThreadStatements(pThread);

    CFunctionCallAssignmentStatement roundMaxNondetAssignment =
        VerifierNondetFunctionType.buildNondetIntegerAssignment(
            options, SeqIdExpressions.ROUND_MAX);
    CFunctionCallStatement roundMaxGreaterZeroAssumption =
        SeqAssumeFunction.buildAssumeFunctionCallStatement(
            utils
                .binaryExpressionBuilder()
                .buildBinaryExpression(
                    SeqIdExpressions.ROUND_MAX,
                    SeqIntegerLiteralExpressions.INT_0,
                    BinaryOperator.GREATER_THAN));
    CExpressionAssignmentStatement roundReset = NondeterministicSimulationBuilder.buildRoundReset();

    ImmutableList.Builder<CExportStatement> rStatements = ImmutableList.builder();
    pcUnequalExitAssumption.ifPresent(s -> rStatements.add(new CStatementWrapper(s)));
    nextThreadStatements.ifPresent(l -> l.forEach(s -> rStatements.add(new CStatementWrapper(s))));
    rStatements.add(new CStatementWrapper(roundMaxNondetAssignment));
    rStatements.add(new CStatementWrapper(roundMaxGreaterZeroAssumption));
    rStatements.add(new CStatementWrapper(roundReset));
    return rStatements.build();
  }
}
