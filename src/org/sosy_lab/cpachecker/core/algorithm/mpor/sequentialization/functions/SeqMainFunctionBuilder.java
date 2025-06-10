// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.function_call.SeqFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqMainFunctionBuilder {

  public static SeqMainFunction buildMainFunction(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      Optional<BitVectorVariables> pBitVectorVariables,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    // create clauses in main method
    ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> clauses =
        SeqThreadStatementClauseBuilder.buildClauses(
            pOptions,
            pSubstitutions,
            pSubstituteEdges,
            pBitVectorVariables,
            pPcVariables,
            pThreadSimulationVariables,
            pBinaryExpressionBuilder,
            pLogger);
    return new SeqMainFunction(
        pOptions,
        pSubstitutions,
        clauses,
        pBitVectorVariables,
        pPcVariables,
        pBinaryExpressionBuilder,
        pLogger);
  }

  protected static Optional<SeqFunctionCallStatement> buildThreadActiveAssumption(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.threadLoops && !pOptions.threadLoopsNext) {
      // without threadLoopsNext, no assumption is required due to if (pc != -1) ... check
      return Optional.empty();
    }
    if (pOptions.scalarPc) {
      CBinaryExpression threadActiveExpression =
          SeqExpressionBuilder.buildPcUnequalExitPc(
              pPcVariables.getPcLeftHandSide(pThread.id), pBinaryExpressionBuilder);
      SeqFunctionCallExpression assumeCallExpression =
          SeqAssumptionBuilder.buildAssumeCall(threadActiveExpression);
      return Optional.of(assumeCallExpression.toFunctionCallStatement());
    }
    return Optional.empty();
  }
}
