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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqFunctionBuilder {

  public static ImmutableList<LineOfCode> buildFunctionDefinitions(
      MPOROptions pOptions,
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>>
          pReturnPcVariables,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rFunctionDefinitions = ImmutableList.builder();
    // custom function definitions: reach_error(), assume(), main()
    SeqReachErrorFunction reachError = new SeqReachErrorFunction();
    rFunctionDefinitions.addAll(reachError.buildDefinition());
    SeqAssumeFunction assume = new SeqAssumeFunction(pBinaryExpressionBuilder);
    rFunctionDefinitions.addAll(assume.buildDefinition());
    SeqMainFunction mainFunction =
        buildMainFunction(
            pOptions,
            pSubstitutions,
            pSubstituteEdges,
            pReturnPcVariables,
            pPcVariables,
            pThreadSimulationVariables,
            pBinaryExpressionBuilder,
            pLogger);
    rFunctionDefinitions.addAll(mainFunction.buildDefinition());
    return rFunctionDefinitions.build();
  }

  private static SeqMainFunction buildMainFunction(
      MPOROptions pOptions,
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>>
          pReturnPcVariables,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    // create case clauses in main method
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> caseClauses =
        SeqCaseClauseBuilder.buildCaseClauses(
            pSubstitutions,
            pSubstituteEdges,
            pReturnPcVariables,
            pPcVariables,
            pThreadSimulationVariables,
            pBinaryExpressionBuilder,
            pLogger);
    // optional: include POR assumptions
    Optional<ImmutableList<SeqFunctionCallExpression>> porAssumptions =
        pOptions.partialOrderReduction
            ? Optional.of(
                SeqAssumptionBuilder.createPORAssumptions(
                    caseClauses, pPcVariables, pBinaryExpressionBuilder))
            : Optional.empty();
    ImmutableList<SeqFunctionCallExpression> threadSimulationAssumptions =
        SeqAssumptionBuilder.createThreadSimulationAssumptions(
            pPcVariables, pThreadSimulationVariables, pBinaryExpressionBuilder);
    return new SeqMainFunction(
        pOptions,
        pSubstitutions.size(),
        threadSimulationAssumptions,
        porAssumptions,
        caseClauses,
        pPcVariables,
        pBinaryExpressionBuilder);
  }
}
