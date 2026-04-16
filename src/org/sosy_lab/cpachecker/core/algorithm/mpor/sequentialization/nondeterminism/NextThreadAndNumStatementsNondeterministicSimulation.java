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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;

class NextThreadAndNumStatementsNondeterministicSimulation
    extends NextThreadNondeterministicSimulation {

  NextThreadAndNumStatementsNondeterministicSimulation(
      MPOROptions pOptions,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils) {

    super(pOptions, pMachineModel, pMemoryModel, pGhostElements, pClauses, pUtils);
  }

  @Override
  public CCompoundStatement buildPrecedingStatements(MPORThread pThread)
      throws UnrecognizedCodeException {

    Optional<CExportStatement> pcStatement = tryBuildPcPrecedingStatement(pThread);
    Optional<ImmutableList<CExportStatement>> nextThreadStatements =
        tryBuildNextThreadPrecedingStatements(pThread);

    ImmutableList<CCompoundStatementElement> numStatementsNondeterministicStatements =
        NondeterministicSimulationBuilder.buildNumStatementsNondeterministicPrecedingStatements(
            options, pThread, utils.binaryExpressionBuilder());

    CFunctionCallAssignmentStatement roundMaxNondetAssignment =
        VerifierNondetFunctionType.buildNondetIntegerAssignment(
            options, SeqIdExpressions.ROUND_MAX);
    CFunctionCallStatement roundMaxGreaterZeroAssumption =
        SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
            utils
                .binaryExpressionBuilder()
                .buildBinaryExpression(
                    SeqIdExpressions.ROUND_MAX,
                    CIntegerLiteralExpression.ZERO,
                    BinaryOperator.GREATER_THAN));

    ImmutableList.Builder<CCompoundStatementElement> rStatements = ImmutableList.builder();
    nextThreadStatements.ifPresent(l -> rStatements.addAll(l));
    pcStatement.ifPresent(s -> rStatements.add(s));
    rStatements.addAll(numStatementsNondeterministicStatements);
    rStatements.add(new CStatementWrapper(roundMaxNondetAssignment));
    rStatements.add(new CStatementWrapper(roundMaxGreaterZeroAssumption));
    return new CCompoundStatement(rStatements.build());
  }
}
